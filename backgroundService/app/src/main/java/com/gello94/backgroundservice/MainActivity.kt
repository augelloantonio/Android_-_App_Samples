package com.gello94.backgroundservice

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // Set variables
    private val REQUEST_POST_NOTIFICATION = 1 // Post notification permit variable

    var run = false
    var counter = 0
    var isServiceStarted = false

    lateinit var MainHandler: Handler // Set handler to initialize later

    // Build runnable handler to run a task every second - 1000 msec
    private val updateMainTask = object : Runnable {
        override fun run() {
            mainHandler()
            MainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPushNotificationPermissions() // Ask for push notification or the notification won't appear - Required if Android > 32

        MainHandler = Handler(Looper.getMainLooper()) // Initialize handler

        // Build a start and stop button to start service and count and to stop it and to show or hide the opposite button
        startButton.setOnClickListener{
            MainHandler.post(updateMainTask)
            run = true
            stopButton.visibility = View.VISIBLE
            startButton.visibility = View.GONE

            if (!isServiceStarted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isServiceStarted = true
                    startForegroundService(Intent(this, NotificationService::class.java))

                } else {
                    isServiceStarted = true
                    startService(Intent(this, NotificationService::class.java))
                }
            }
        }

        stopButton.setOnClickListener{
            MainHandler.removeCallbacks(updateMainTask)
            run = false
            stopButton.visibility = View.GONE
            startButton.visibility = View.VISIBLE

            stopService(Intent(this, NotificationService::class.java))
            isServiceStarted = false
        }
    }

    fun mainHandler(){
        if (run){
            counter += 1
            countText.text = counter.toString()
        }
    }

    fun requestPushNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= 32) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATION
            )
        }
    }
}