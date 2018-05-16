package com.allclearweather.sensorlibrary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println("startdatareceiver onreceive, starting service")
        val newIntent = Intent(context, SensorForegroundService::class.java)
        context?.startService(newIntent)
    }


}