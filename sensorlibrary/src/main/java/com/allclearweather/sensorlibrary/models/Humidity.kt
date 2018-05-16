package com.allclearweather.sensorlibrary.models

class Humidity(time: Long,
               humidity: Double,
               latitude : Double,
               longitude: Double): Observation(timeRecorded = time,
        observation = humidity,
        latitude = latitude,
        longitude = longitude) 