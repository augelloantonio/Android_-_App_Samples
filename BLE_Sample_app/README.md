# BLE Sample APP.

## PART 1 - DISCOVER A BLE DEVICE

To Discover the BLE Devices we need first to set in our AndroidManifest file the following permissions:
  
```
  <uses-permission android:name="android.permission.BLUETOOTH" />
   <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
   <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
    
I decided to use a Kotlin Object to use my Scan Object everywhere in the APP, as for be used in a self connection function on app opening.
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

Now we can proceed to call the function scan() i wrote in object present in [BLEManagement.kt](https://github.com/gello94/Android_-_App_Samples/blob/main/BLE_Sample_app/app/src/main/java/com/gello94/ble_sample_app/BLEManagement.kt)

The function uses an Handler() to keep scanning until a given time (SCAN_PERIOD) isn't reached (a Progress Bar will let the user know that the phone is still scanning or if the scan has ended).

We initialize the BluetoothAdapter and the BluetoothLeScanner.

```
private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            MainActivity.mContext!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager =
                MainActivity.mContext!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }
```

Then we create a scanCallback that will override the onScanResult to let us show the results and manage the devices as we prefer.

```
private val scanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            println("Entered scan callback fun")
            val indexQuery = MainActivity.scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                MainActivity.scanResults[indexQuery] = result
            } else {

                with(result.device) {
                    println(result)
                    println("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    var dName = name
                    var dAddress = address
                    if(name==null){
                        dName = "Unknown"
                    }
                    if(address==null){
                        dAddress = "Unknown"
                    }
                    MainActivity.mDeviceList.add(dName)
                    MainActivity.addressList.add(dAddress)
                }
                stopBleScan()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("onScanFailed: code $errorCode")
        }
    }
```
Now we can access the devices contained into the lists from the MainActivity.

The DeviceManagement() function ensure to show us on the MainActivity the BLE Device List we just discovered.

This method is called in an Handler() to ensure the list is always update. It can be done as well with EventBus or other methods but for this sample i've choose the fastest way.

```
val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                println(isScanning)
                if  (!isScanning) {
                    if(mDeviceList.size>0) {
                        deviceManagement(this@MainActivity)
                    }
                }
                mainHandler.postDelayed(this, 1500)
            }
        })
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
