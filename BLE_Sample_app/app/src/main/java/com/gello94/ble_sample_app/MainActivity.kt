package com.gello94.ble_sample_app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    companion object {
        var mContext: Context? = null
        val scanResults = mutableListOf<ScanResult>()
        val mDeviceList = ArrayList<String>()
        val addressList = ArrayList<String>()
        var isScanning = false
        var scanEnd = false
    }

    var deviceName: String = ""
    var address: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this
        val connBTN = findViewById<Button>(R.id.connectBtn)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        checkPermissions(this, this)

        connBTN.setOnClickListener {
            scanEnd = false
            isScanning = false
            BLEManagement.scan(progressBar)
        }

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
    }

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

    fun deviceManagement(ctx: Context) {

        println(mDeviceList)

        val deviceListName = ArrayList<String>()

        for (i in 0..mDeviceList.size-1){
            val device = "name: " + mDeviceList[i] + ", address: " + addressList[i]
            deviceListName.add(device)
        }

        if (deviceListName.size > 0) {
            val listView = findViewById<ListView>(R.id.discoveredDevices)
            listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1, deviceListName
            )

            listView.setOnItemClickListener { parent, view, position, id ->

                address = addressList[position]
                deviceName = mDeviceList[position]

                Toast.makeText(ctx, "Click on ${address}", Toast.LENGTH_LONG).show()
            }
        }
    }
}