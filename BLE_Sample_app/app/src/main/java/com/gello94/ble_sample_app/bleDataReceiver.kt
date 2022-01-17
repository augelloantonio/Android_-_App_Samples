package com.gello94.ble_sample_app

import android.bluetooth.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.util.*

class bleDataReceiver : AppCompatActivity() {

    var dataUUID_service = ""
    var descriptor_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Generic Descriptor UUID
    var dataUUID_characteristic = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // Your Device Characteristic UIID

    val servicecesInventory = LinkedHashMap<String, MutableList<BluetoothGattCharacteristic>>()

    var deviceGatt: BluetoothDevice? = null

    var chars = ArrayList<UUID>()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_data_receiver)

        val m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(m_bluetoothAdapter == null) {
            Toast.makeText(this, "this device doesn't support bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)!!
        val m_name = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)!!

        deviceGatt = m_bluetoothAdapter.getRemoteDevice(
                m_address
        )

        if (deviceGatt!=null) {
            deviceGatt!!.connectGatt(this, false, gattCallback, 2)
        }

    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    // TODO: Store a reference to BluetoothGatt
                    gatt.discoverServices()

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w(
                        "BluetoothGattCallback",
                        "Error $status encountered for $deviceAddress! Disconnecting..."
                )

                gatt.disconnect()
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w(
                        "BluetoothGattCallback",
                        "Discovered ${services.size} services for ${device.address}"
                )

                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here

                for (service in gatt!!.services){
                    for (char in service.characteristics){
                        subscribeToCharacteristics(gatt!!, char.uuid.toString(), service.uuid)
                    }
                }
            }
        }

        override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

            for(service in gatt!!.services){
                servicecesInventory[service.uuid.toString()] = service.characteristics
            }

            Log.d("onDescWrite", "Notify Enabled")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("TAG", "Callback: Wrote GATT Descriptor successfully.");

                for ((k,v) in servicecesInventory) {
                    for(u in v){
                        chars.add(u.uuid)
                    }

                    if(chars.size>0){
                        chars.removeAt(0);
                        subscribeToCharacteristicsList(gatt!!, chars, UUID.fromString(k))
                    }
                }

            }
            else{
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
                        Log.i("BluetoothGattCallback","Read characteristic $uuid:\n${value.toHexString()}" )
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
            dataUUID_service = service.uuid.toString()
        }
    }

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
        if (descriptor!=null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private fun subscribeToCharacteristicsList(
            gatt: BluetoothGatt,
            chars: ArrayList<UUID>,
            service:UUID
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
        } catch (e:Exception){
            Log.d("E/subscription: ", e.toString())
        }
    }

    fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
}