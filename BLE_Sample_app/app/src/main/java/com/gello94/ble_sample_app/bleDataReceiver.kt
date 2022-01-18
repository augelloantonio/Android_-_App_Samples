package com.gello94.ble_sample_app

import android.bluetooth.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.util.*

class bleDataReceiver : AppCompatActivity() {

    var descriptor_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Generic Descriptor UUID
    val dataUUID_service = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val dataUUID_characteristic = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val dataUUID_characteristic3 = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    val dataUUID_characteristic4 = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e")
    val dataUUID_characteristic6 = UUID.fromString("6e400006-b5a3-f393-e0a9-e50e24dcca9e")
    val servicecesInventory = LinkedHashMap<String, MutableList<BluetoothGattCharacteristic>>()

    var deviceGatt: BluetoothDevice? = null

    var chars = ArrayList<UUID>() // If you already have your device command and UUIDs please populate this list

    var mName = ""
    var mAddress = ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_data_receiver)

        val deviceAddressText = findViewById<TextView>(R.id.DeviceAddress)
        val deviceNameText = findViewById<TextView>(R.id.DeviceName)
        val deviceConnectionStatusText = findViewById<TextView>(R.id.deviceConnectionStatus)
        val m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Check If Bluetooth is supported by Android Device
        if (m_bluetoothAdapter == null) {
            Toast.makeText(this, "this device doesn't support bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve Device Name and Adddress from Extra Argument passed into the Intent
        mAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)!!
        mName = intent.getStringExtra(MainActivity.EXTRA_NAME)!!

        // Get the device Gatt
        deviceGatt = m_bluetoothAdapter.getRemoteDevice(
            mAddress
        )

        //Show on Activity Layout the Device Info
        deviceNameText.setText(mName)
        deviceAddressText.setText(mAddress)
        deviceConnectionStatusText.setText("Connecting ...")

        // Connect to Device GATT
        if (deviceGatt != null) {
            deviceGatt!!.connectGatt(this, false, gattCallback, 2)
        }

    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            val deviceConnectionStatusText = findViewById<TextView>(R.id.deviceConnectionStatus)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")

                    // If the connection is successful we can discover GATT services

                    gatt.discoverServices() // Goes into -> onServicesDiscovered

                    // Set Connection Status to Connected on Screen

                    runOnUiThread(Runnable {
                        deviceConnectionStatusText.setText("Connected")
                    })

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")

                    // Disconnection successful
                    gatt.close()
                    runOnUiThread(Runnable {
                        deviceConnectionStatusText.setText("Disconnected ...")
                    })

                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )

                // Some error encountered into the connection, will disconnect
                runOnUiThread(Runnable {
                    deviceConnectionStatusText.setText("Disconnecting ...")
                })

                gatt.disconnect()
                gatt.close()

                runOnUiThread(Runnable {
                    deviceConnectionStatusText.setText("Disconnected ...")
                })
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w(
                    "BluetoothGattCallback",
                    "Discovered ${services.size} services for ${device.address}"
                )

                // Can use this method to print all services
                printGattTable()


                /**
                 * Subscribe to the wanted Characteristic, if want to connect to more chars
                 * and you have already a list of UUIDs you can use them, or you can retrieve them as
                 * I show into "onDescriptorWrite". But I am sure you already know on which characteristics
                 * subscribe to get your data.
                 * In case you have already your characteristics list skip this

                for (service in gatt!!.services) {
                    for (char in service.characteristics) {
                        subscribeToCharacteristics(gatt!!, char.uuid.toString(), service.uuid)
                    }
                }

                 **/


                /**
                 *
                 * The following code is to use instead of the upper code to write to the device a permission code
                 * to start the communication.
                 * Usual BLE devices have specific codes to allow communication.
                 * After the writeToDevice method the next step is going into onCharacteristicWrite
                 *
                 **/

                val msg = byteArrayOf(0x00.toByte()) // The start communication code if needed
                writeToDevice(gatt, msg, dataUUID_service, dataUUID_characteristic) // Use the service and its characteristic UUID where to send the permission code

            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // println("ENTERED onCharacteristicWrite")

            chars.add(dataUUID_characteristic3)
            chars.add(dataUUID_characteristic4)
            chars.add(dataUUID_characteristic6)

            // Subscribe to the characteristic list -> It will start with the first and loop over all the others
            // into onDescriptorWrite method.
            subscribeToCharacteristicsList(gatt, chars, dataUUID_service)
        }


        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

            /****
             *
             * If you want to subscribe to all the characteristics in a specific service
             * you can loop over the discovered services , add an if condition with your service name
             * and use a list, I used a map to save services and relative characteristics.
             *
             * Then from the service I want I will get all the characteristics and loop
             * into a list I made removing the characteristics I already subscribed
             *
             ****/
            for (service in gatt!!.services) {
                servicecesInventory[service.uuid.toString()] = service.characteristics
            }

            Log.d("onDescWrite", "Notify Enabled")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("TAG", "Callback: Wrote GATT Descriptor successfully.");

                /**
                * If the GATT status is 0 (connected) we can subscribe to the characteristics
                * Note: Use the Service UIID containing the Characteristics UUID you want to subscribe
                * If the characteristic is only one you can just use single UUID val and subscribe to that
                 **/

                /*
                for ((k, v) in servicecesInventory) {
                    for (u in v) {
                        chars.add(u.uuid)
                    }

                    if (chars.size > 0) {
                        chars.removeAt(0);
                        subscribeToCharacteristicsList(gatt!!, chars, UUID.fromString(k))
                    }
                }
                 */

                /**
                 * I believe you already have your characteristic list with all your characteristic UUID into it.
                 * If so use the following code instead of the upper one.
                 */

                if(chars.size>0){
                    chars.removeAt(0);
                    subscribeToCharacteristicsList(gatt!!, chars, dataUUID_service)
                }

            } else {
                Log.d("TAG", "Callback: Error writing GATT Descriptor: " + status);
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            charac: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, charac)
            Log.d("CHARAC", "Characteristic Changed")
            onCharacteristicRead(gatt, charac, BluetoothGatt.GATT_SUCCESS)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(
                            "BluetoothGattCallback",
                            "Read characteristic $uuid:\n${value.toHexString()}"
                        )
                        val deviceReceivedDataText = findViewById<TextView>(R.id.deviceDataReceived)


                        runOnUiThread(Runnable {
                            deviceReceivedDataText.setText("${value.toHexString()}")
                        })


                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(
                            "BluetoothGattCallback",
                            "Characteristic read failed for $uuid, error: $status"
                        )
                    }
                }
            }
        }

    }


    /**
     * Print a Service and its Characteristics Table
     **/
    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i(
                "printGattTable",
                "No service and characteristic available, call discoverServices() first?"
            )
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i(
                "printGattTable",
                "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    /**
     * Connect to a single UUIDs
     **/
    private fun subscribeToCharacteristics(
        gatt: BluetoothGatt,
        chars: String, service: UUID
    ) {
        if (chars === null) return

        val characteristic = gatt.getService(service).getCharacteristic(
            UUID.fromString(chars)
        )
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic!!.getDescriptor(descriptor_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    /**
     * Connect to a list of UUIDs of a specific Service, remove the items once you are subscribed
     **/

    private fun subscribeToCharacteristicsList(
        gatt: BluetoothGatt,
        chars: ArrayList<UUID>,
        service: UUID
    ) {
        try {
            if (chars.size === 0) return
            val characteristic = gatt.getService(service).getCharacteristic(
                chars.get(0)
            )
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic!!.getDescriptor(descriptor_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        } catch (e: Exception) {
            Log.d("E/subscription: ", e.toString())
        }
    }


    /**
     * Useful ByteArray to HEX String Converter.
     * Gets a ByteArray as input and returns a HEX String
     **/
    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }


    /**
     * Useful method to write HEX messages to the device.
     * Most BLE devices requires you to write messages as for example connection request code or
     * connection request permitts, or also some specific code to send to a specific characteristic
     * to get the data.
     * In case you need to write to the device I recommend to use this int the "onServiceDiscovered".
     **/
    private fun writeToDevice(
        gatt: BluetoothGatt,
        msg: ByteArray,
        UUID_service: UUID,
        UUID_characteristic: UUID
    ) {
        val newcharacteristic =
            gatt!!.getService(UUID_service).getCharacteristic(UUID_characteristic)
        val newvalue = msg
        newcharacteristic!!.value = newvalue
        gatt!!.writeCharacteristic(newcharacteristic)
    }
}