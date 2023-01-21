package com.gello94.backgroundservice


import android.Manifest
import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

public class NotificationService: Service() {

    fun onCreate(intent: Intent) {
        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent!=null){
            createNotify(intent)
        }
        return super.onStartCommand(intent, flags, startId);
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    fun createNotify(intent: Intent){
        val channelId = "Notification from Service"
        val channel =
            NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        (getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            1, notificationIntent, FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App in running in background")
            .setSmallIcon(androidx.appcompat.R.drawable.abc_btn_radio_material, 3)
            .setContentIntent(pendingIntent)
            .setNotificationSilent()
            .setOngoing(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            startForeground(5, notification)
        }
    }
}