# Charting Sample APP.

![Main Page](https://github.com/gello94/Android_-_App_Samples/blob/main/images/chart_sample_app.png)

This is a sample app that uses EventBus to plot smooth real time data on Android using MPAndroidChart.

In this sample app the real time data can be added by inserting a value in an input text field and send by tapping a button.

## Dependencies
```
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    implementation 'org.greenrobot:eventbus:3.2.0'
```

## Step by step 
The real time data, same as the use of data from Bluetooth/BLE sensors or any others, has the same logic behind, and what I do in  is to use the AddEntry function in a subscribed event listener.

### The necessary steps to follow are:

1. Create the EventBus (I would make a new object file to make eventbus ordered):
```
       data class DataEvent(var dataToSend: Float){}
```

Of course, you need to register and subscribe to this events. And how to?

2. Register your eventbus inside your bluetooth receiver you need to register the data to subscribe (where you get the data package from the bluetooth to be clear):
```
       val eventData: DataEvent = DataEvent(yourData)
       EventBus.getDefault().post(eventData)
```

3. Subscribe, you need to subscribe of course now or you won't see any data being saved into your event class.
   Because you need to plot data, in your ElectromyographyAnalysis Class you need to subscribe to the event:
```
       @Subscribe(threadMode = ThreadMode.MAIN)
       public fun onDataReceived(event: DataEvent) {
           if (event.dataToSend != null) {
               dataToPlot = event.dataToSend
           }
           addEntry(dataToPlot) // THIS WILL CALL YOUR ADDENTRY()
       }
```

This way you will send to the AddEntry() method every single bit the bluetooth receives and plot them.