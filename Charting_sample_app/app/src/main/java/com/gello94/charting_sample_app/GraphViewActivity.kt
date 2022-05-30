package com.gello94.charting_sample_app

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.graph_view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class GraphViewActivity : AppCompatActivity() {

    // Load the starting chart necessary variable
    lateinit var chart: LineChart
    var yArray = ArrayList<Entry>()
    lateinit var set1: LineDataSet
    var xLabel = ArrayList<String>()
    var firststartedSessionTs: Long = 0

    // Let's create the event bus data listener
    data class liveGraphTimeListener(var time:String){
    }
    data class liveGraphDataListener(var dataToPlot:Float){
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.graph_view)

        // Keep the screen awake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Load the chart from the layout view
        chart = graph

        //Button to set which data do you want to send to the graph
        sendNumber.setOnClickListener {
            sendDataToGraph()
        }
    }

    private fun sendDataToGraph(){
        val data = dataToSend.text.toString().toFloat()

        // Post data to the subscriber
        val dataToPlot: liveGraphDataListener = liveGraphDataListener(data)
        EventBus.getDefault().post(dataToPlot)
    }

    fun addEntry(datatoplot: Float) {

        val data = chart.data

        // Add data to the graph in x and y axix
        if (data != null) {
            var set = data.getDataSetByIndex(0)
            if (set1 == null) {
                data.addDataSet(set)
            }

            data.addEntry(
                Entry(
                    set.entryCount.toFloat(),
                    datatoplot
                ), 0
            )

            // Get elapsed time to plot as xAxix in the graph
            val dataPointsTime = getTimestampForXAxis()

            // Create x axix label - I used data time as label in this sample
            xLabel.add(dataPointsTime)

            // Notify the graph data changed
            data.notifyDataChanged()

            // move to the latest entry
            chart.moveViewToX(data.entryCount.toFloat())
            // limit the number of visible entries

            // move to the latest entry
            chart.moveViewToX(data.entryCount.toFloat())

            // Set max and min range view
            chart.setVisibleXRangeMaximum(250f)
            chart.setVisibleXRangeMinimum(250f)

            chart.getAxisRight().setEnabled(false);
            // move to the latest entry
            chart.moveViewToX(data.entryCount.toFloat())

            chart.setAutoScaleMinMaxEnabled(true);

            chart.axisLeft.removeAllLimitLines()
            chart.axisLeft.resetAxisMaximum()
            chart.axisLeft.resetAxisMinimum()

            chart.getAxisLeft().setTextColor(Color.BLACK); // left y-axis
            chart.getXAxis().setTextColor(Color.BLACK);
            chart.getLegend().setTextColor(Color.BLACK);

            chart.notifyDataSetChanged(); // let the chart know it's data changed

            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f

            xAxis.textSize = 8f
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabel)

            chart.invalidate()

            xLabel.clear()
        }
    }

    private fun getTimestampForXAxis():String{
        val startSessionTS = firststartedSessionTs
        val date = Calendar.getInstance().time
        val actualTs = date.getTime() / 1000
        var secondLapsed= actualTs-startSessionTS

        var minuteLapsed= secondLapsed/60
        var hourLapsed = minuteLapsed/60

        var stringSec = ""
        var stringMin = ""
        var stringHour =""

        if (secondLapsed%60 < 10){
            stringSec = "0"+secondLapsed%60
        } else{
            stringSec = (secondLapsed%60).toString()
        }
        if (minuteLapsed%60 < 10){
            stringMin = "0"+minuteLapsed%60
        } else{
            stringMin = (minuteLapsed%60).toString()
        }
        if (hourLapsed%60 < 10){
            stringHour = "0"+hourLapsed%60
        } else{
            stringHour = (hourLapsed%60).toString()
        }

        val dataPointsTime = stringHour + ":" + stringMin + ":" + stringSec

        val timeLiveEventData: liveGraphTimeListener = liveGraphTimeListener(dataPointsTime)
        EventBus.getDefault().post(timeLiveEventData)

        return dataPointsTime
    }

    fun renderChartOnline() {
        var i = 0

        yArray.add(Entry(0.toFloat(), 1.20!!.toFloat()))

        set1 = LineDataSet(yArray, "")

        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setLineWidth(2f)
        set1.setColor(Color.RED);

        val data = LineData(set1)
        chart.setData(data)

        chart.getAxisRight().setEnabled(false);

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // only intervals of 1 day

        xAxis.textSize = 8f

        xAxis.valueFormatter = IndexAxisValueFormatter(xLabel)

        chart.getAxisLeft().setTextColor(Color.WHITE); // left y-axis
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getLegend().setTextColor(Color.WHITE);

        chart.invalidate()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onTimeReceived(event: liveGraphTimeListener){
        if (event.time!=null){
            liveTime.setText("Time: " + event.time)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onDataToPlotReceived(event: liveGraphDataListener){
        if (event.dataToPlot!=null){
            // Add data to the graph every time you receive a new data from the event bus listener
            addEntry(event.dataToPlot)
        }
    }

    override fun onStart() {
        super.onStart()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onResume() {
        super.onResume()

        // This will load an empty starting chart where data are going to be added
        renderChartOnline()

        //This will check if thr eventbus is registered or not
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

}