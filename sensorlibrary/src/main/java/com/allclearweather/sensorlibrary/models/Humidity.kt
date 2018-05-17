package com.allclearweather.sensorlibrary.models

/**
 * Store an individual reading from the humidity sensor
 */
class Humidity(time: Long,
               humidity: Double,
               latitude : Double,
               longitude: Double): Observation(timeRecorded = time,
        observation = humidity,
        latitude = latitude,
        longitude = longitude) 