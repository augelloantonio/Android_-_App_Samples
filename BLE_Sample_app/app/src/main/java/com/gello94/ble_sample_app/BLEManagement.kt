package com.gello94.ble_sample_app

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

object BLEManagement {
    val SCAN_PERIOD: Long = 10000

    private val handler = Handler()

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

    private val scanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = MainActivity.scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                MainActivity.scanResults[indexQuery] = result
            } else {
                with(result.device) {
                    Log.d("scan result: ", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    var dName = name
                    var dAddress = address

                    if(name==null){
                        dName = "Unknown"
                    }
                    if(address==null){
                        dAddress = "Unknown"
                    }

                    // Check if device already in list
                    if (!MainActivity.mDeviceList.contains(dName)){
                        MainActivity.mDeviceList.add(dName)
                    }

                    if (!MainActivity.addressList.contains(dAddress)){
                        MainActivity.addressList.add(dAddress)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("scan result: ", "onScanFailed: code $errorCode")
        }
    }

    fun scan(progress_bar: ProgressBar){
        bluetoothAdapter.cancelDiscovery()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
        startScanProcess(progress_bar)
    }

    fun startScanProcess(progress_bar: ProgressBar){
        if (!MainActivity.isScanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                MainActivity.isScanning = false
                progress_bar.visibility = View.GONE
                stopBleScan()
            }, SCAN_PERIOD)
            MainActivity.isScanning = true
            startProcess()
            progress_bar.visibility = View.VISIBLE
        } else {
            MainActivity.isScanning = false
            stopBleScan()
            progress_bar.visibility = View.GONE
        }
    }

    private fun stopBleScan() {
        bluetoothLeScanner!!.stopScan(scanCallback)
        MainActivity.isScanning = false
    }

    fun startProcess(){
        MainActivity.mDeviceList.clear()
        MainActivity.addressList.clear()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

            if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isDiscovering) {
                MainActivity.scanResults.clear()
                bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
                MainActivity.isScanning = true
            }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(MainActivity.mContext!!, enableBtIntent, null)
        }
    }

}