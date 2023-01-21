# Foreground Task APP Sample.

## PART 1 - Permits
<img src="https://github.com/gello94/Android_-_App_Samples/blob/main/images/background_app_main.png" width="256" height="455">

To allow the foreground services and the notification you need to add the following permissions in your manifest file:
  
```
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

The Push Notification permit has been introduced in Android sdk 32 and has to be allowed from the user.

We can prompt the user to allow push notifications by inserting the following permission handler in the main activity.

```
fun requestPushNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= 32) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATION
            )
        }
    }
```

## PART 2 - The notification Service
Now we can proceed to build the notification service to build the persistent notification to show and allow the app working in the foreground all the time.

We create a new Kotlin Class named NotificatioService and we declare it as a Service:

```
public class NotificationService: Service() {
    // your code here - the code is in the repository
}
```

So we first build a NotificationChannel:

```
val channelId = "Notification from Service"
val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
```

Then create a Notification Service:

```
(getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
```

Now we declare a PendingIntent:

```
val notificationIntent = Intent(this, MainActivity::class.java)
val pendingIntent = PendingIntent.getActivity(
        this,
        1, notificationIntent, FLAG_IMMUTABLE)
```

And finally we build the notification:

```
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
```

## PART 3 - Use the Notification Service
<img src="https://github.com/gello94/Android_-_App_Samples/blob/main/images/background_notification.png" width="256" height="455">

In the Main Activity I used a runnable Handler to run a task every second and update the text view.
The Layout is very simple, there is a text view containing the text of the counter, a start button that hide on start press and a stop button that shows.
On Start press the counter start counting +1 every seconds and the service is called as follow:

```
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    isServiceStarted = true
    startForegroundService(Intent(this, NotificationService::class.java))
} else {
    isServiceStarted = true
    startService(Intent(this, NotificationService::class.java))
}
```

We use this VERSION_CODES check because startForegroundService has been introdiced in sdk 26.

Then we stop the foreground service on Stop pressing by caling the stopService as follow:

```
stopService(Intent(this, NotificationService::class.java))
```

Mind that to prevent the app from being closed by the system you need to ask the user also to disable the battery optimization for your app.