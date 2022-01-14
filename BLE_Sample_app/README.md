# BLE Sample APP.

## PART 1 - DISCOVER A BLE DEVICE

To Discover the BLE Devices we need first to set in our AndroidManifest file the following permissions:
  
```
  <uses-permission android:name="android.permission.BLUETOOTH" />
   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
    
I decided to use a Kotlin Object to use my Scan Object everywhere in the APP, ad for a self connection on app onening.
But for this Sample APP I will call the scan() function on the "Scan" button click.

Before we call the scan() function what will start our scan it is important to check if the upcited permissions were given and if not we need to promp a message to do that. To achieve this I use the following check permissions function.

```
fun checkPermissions(activity: Activity?, context: Context?) {
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )
        if (!hasPermissions(context, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity!!, PERMISSIONS, PERMISSION_ALL)
        }
    }

    fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }
```

## PART 2 - Read Data From BLE DEVICE

The Flow i follow in the app is the following:
 1. Connect Gatt;
 2. Discover Services;
 3. Write To Characteristic;
 4. Subscribe to Notification;
 5. Read Characteristic from notification

Going deeper into the connection and using some code:

After you connect to the GATT you call onConnectionStateChange to listen for changes in the gatt connection:

```
    private val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
    val deviceAddress = gatt.device.address
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                        // NOW DISCOVER SERVICES
                        gatt.discoverServices()
    
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    }
                } else {
                    Log.w(
                        "BluetoothGattCallback",
                        "Error $status encountered for $deviceAddress! Disconnecting..."
                    ) 
            }
```

If the GATT is connected succesfully it will discover services.

At this step you can write to the characteristic as follow:
```
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        with(gatt) {
            Log.w(
                "BluetoothGattCallback",
                "Discovered ${services.size} services for ${device.address}"
            )
    
            val msg = byteArrayOf(0x00.toByte())
            val newcharacteristic = gatt!!.getService(dataUUID_service).getCharacteristic(
                dataUUID_characteristic
            )
            newcharacteristic!!.value = msg
            gatt!!.writeCharacteristic(newcharacteristic)        
        }
    }
```
This will let you go on the next step, the onCharacteristicWrite listener:
```
    override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
    
                val characteristic = gatt.getService(dataUUID_service).getCharacteristic(
                dataUUID_characteristic
            )
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic!!.getDescriptor(descriptor_UUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
            }


Writing the characteristic will let you go into the onCharacteristicChanged listener that will give you back the data from the ble device and in which you can use the event bus to use your data. 

    override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                charac: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicChanged(gatt, charac)
                // Log.d("CHARAC", "Characteristic Changed")
                onCharacteristicRead(gatt, charac, BluetoothGatt.GATT_SUCCESS)
            }
```
Where onCharacteristicRead should look like:
```
    override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                with(characteristic) {
                    when (status) {
                        BluetoothGatt.GATT_SUCCESS -> {
                            Log.i("BluetoothGattCallback","Read characteristic $uuid:\n${value.toHexString()}" )
    
    
                        // value is the read value from ble device
                        // HERE YOU HANDLE THE DATA RECEIVED

    
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
```
Maybe it is not the most efficent way and not the clearest code but it works like a charm.
