package com.allclearweather.sensorlibrary.models

class Temperature(time: Long,
                  temperature: Double,
                  latitude : Double,
                  longitude: Double): Observation(timeRecorded = time,
        observation = temperature,
        latitude = latitude,
        longitude = longitude) 