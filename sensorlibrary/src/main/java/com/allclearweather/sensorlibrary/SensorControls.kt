package com.allclearweather.sensorlibrary

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorControls {

    companion object {

        val UPDATE_INTERVAL = (2 * 1000).toLong()  /* 10 secs */
        val FASTEST_INTERVAL: Long = 2000 /* 2 sec */

        fun startSensorListener(mSensorManager: SensorManager, sensor: Sensor?, sensorEventListener: SensorEventListener) {
            mSensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        fun stopSensorListener(mSensorManager: SensorManager, sensor: Sensor?, sensorEventListener: SensorEventListener) {
            mSensorManager.unregisterListener(sensorEventListener, sensor)
        }

    }
}