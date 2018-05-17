package com.allclearweather.sensorlibrary.models

/**
 * Store an individual reading from the temperature sensor
 */
class Temperature(time: Long,
                  temperature: Double,
                  latitude : Double,
                  longitude: Double): Observation(timeRecorded = time,
        observation = temperature,
        latitude = latitude,
        longitude = longitude) 