package com.allclearweather.sensorlibrary

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.app.NotificationChannel
import android.os.Build


class SensorForegroundService : Service() {

    private var notificationManager: NotificationManager? = null
    private var isRunning = false

    val NOTIFICATION_REQUEST_CODE = 1000

    val channelName = "CHANNEL_ALLCLEAR"



    override fun onCreate() {
        super.onCreate()
        val context = this as Context
        isRunning = false
        createNotificationChannel()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if(!isRunning) {
            var notificationIntent = Intent("com.allclearweather.allclearsensorlibrary.SERVICE_FOREGROUND_NOTIFICATION")
            var pendingIntent = PendingIntent.getActivity(this,  NOTIFICATION_REQUEST_CODE, notificationIntent, 0)



            var notificationBuilder = NotificationCompat.Builder(this, channelName)
                    .setSmallIcon(R.drawable.partlycloudy)
                    .setContentText("Sensor data")
                    .setContentTitle("All Clear Weather")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)


            var notification = notificationBuilder.build()
            notificationManager?.notify(1, notification)

            isRunning = true
            startForeground(1, notification)

        } else {
            isRunning = false
            notificationManager?.cancel(1)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = channelName
            val description = "ChannelDescription"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelName, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }


}