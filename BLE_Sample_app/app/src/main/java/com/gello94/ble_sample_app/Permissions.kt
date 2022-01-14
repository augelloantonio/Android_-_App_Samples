package com.gello94.ble_sample_app

import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

fun Activity.hasPermission(permission: String) = ContextCompat.checkSelfPermission(
    this,
    permission
) == PackageManager.PERMISSION_GRANTED

fun Activity.toast(message: String){
    Toast.makeText(this, message , Toast.LENGTH_SHORT).show()
}