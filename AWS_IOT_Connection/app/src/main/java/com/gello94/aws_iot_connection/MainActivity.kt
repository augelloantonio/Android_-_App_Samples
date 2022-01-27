package com.gello94.aws_iot_connection

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlMeasures = "https://your_endpoint:your_port/topics/your_topic"

        // Check if the app has memory access permissions
        checkPermissions()

        sendMsgBtn.setOnClickListener {

            // Create a json to use as a body
            val json = JSONObject()
            json.put("message", "Hello World")

            // Use async method to post the request
            doAsync{
                AWSConnection.postData(json.toString(), urlMeasures, messageSend)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            false
        }
    }
}