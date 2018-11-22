package com.allclearweather.sensorlibrary

import android.content.Context
import android.hardware.SensorEvent
import com.allclearweather.sensorlibrary.api.SensorApi
import com.allclearweather.sensorlibrary.models.Humidity
import com.allclearweather.sensorlibrary.models.Light
import com.allclearweather.sensorlibrary.models.Pressure
import com.allclearweather.sensorlibrary.models.Temperature
import com.allclearweather.sensorlibrary.util.FileUtil

class DataRecorder {

    companion object {
        fun recordHumidityValues(event: SensorEvent,
                                         latitude: Double,
                                         longitude: Double,
                                         applicationContext: Context,
                                         humidityValues: ArrayList<Humidity>) {
            if(latitude ==0.0) {
                InternalConfig.log("not recording humidity, latitude is 0")
                return
            }

            val eventVal = event.values[0]
            val newHumidity = Humidity(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
            val newData = "${newHumidity.toCSV()}\n"
            humidityValues.add(newHumidity)
            SensorApi.sendHumidityToServer(newHumidity, applicationContext)
            FileUtil.cleanAndSave(applicationContext, "humidity.csv", newData)
        }

        fun recordPressureValues(event: SensorEvent,
                                 latitude: Double,
                                 longitude: Double,
                                 applicationContext: Context,
                                 pressureValues: ArrayList<Pressure>) {
            if(latitude ==0.0) {
                InternalConfig.log("not recording pressure, latitude is 0")
                return
            }
            val eventVal = event.values[0]
            val newPressure = Pressure(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
            val newData = "${newPressure.toCSV()}\n"
            InternalConfig.log("pressure data: $newData")
            pressureValues.add(newPressure)
            SensorApi.sendPressureToServer(newPressure, applicationContext)
            FileUtil.cleanAndSave(applicationContext, "pressure.csv", newData)
        }

        fun recordTemperatureValues(event: SensorEvent,
                                    latitude: Double,
                                    longitude: Double,
                                    applicationContext: Context,
                                    temperatureValues: ArrayList<Temperature>) {
            val eventVal = event.values[0]
            val newTemperature = Temperature(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
            val newData = "${newTemperature.toCSV()}\n"
            temperatureValues.add(newTemperature)
            SensorApi.sendTemperatureToServer(newTemperature, applicationContext)
            FileUtil.cleanAndSave(applicationContext, "temperature.csv", newData)
        }


        fun recordLightValues(event: SensorEvent,
                              latitude: Double,
                              longitude: Double,
                              applicationContext: Context,
                              lightValues: ArrayList<Light>) {
            if(latitude ==0.0) {
                InternalConfig.log("not recording light, latitude is 0")
                return
            }
            val eventVal = event.values[0]
            val newLight = Light(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
            val newData = "${newLight.toCSV()}\n"
            lightValues.add(newLight)
            SensorApi.sendLightToServer(newLight, applicationContext)
            FileUtil.cleanAndSave(applicationContext, "light.csv", newData)
        }

    }
    
    
}