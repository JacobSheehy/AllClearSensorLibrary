package com.allclearweather.sensorlibrary.models

/**
 * Store an individual reading from the pressure sensor
 */
class Pressure(time: Long,
               pressure: Double,
               latitude : Double,
               longitude: Double): Observation(timeRecorded = time,
        observation = pressure,
        latitude = latitude,
        longitude = longitude)