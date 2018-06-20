package com.allclearweather.sensorlibrary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receive intents from other apps / host apps and send them to the service to start.
 */
class StartDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        InternalConfig.log("startdatareceiver onreceive, starting service")
        val newIntent = Intent(context, SensorForegroundService::class.java)
        context?.startService(newIntent)
    }


}