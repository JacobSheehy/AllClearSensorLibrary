package com.allclearweather.sensorlibrary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.content.ContextCompat

/**
 * Receive intents from other apps / host apps and send them to the service to start.
 */
class StartDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            context?.let {
                InternalConfig.log("startdatareceiver onreceive, starting service")
                val newIntent = Intent(context, SensorForegroundService::class.java)
                if(Build.VERSION.SDK_INT>=26 ) {
                    ContextCompat.startForegroundService(it, newIntent)
                } else {
                    it.startService(intent)
                }
            }
        } catch(e: Exception) {
            if(InternalConfig.DEBUG) {
                e.printStackTrace()
            }

        }
    }
}