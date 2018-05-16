package com.allclearweather.sensorlibrary.models

class Pressure(time: Long,
               pressure: Double,
               latitude : Double,
               longitude: Double): Observation(timeRecorded = time,
        observation = pressure,
        latitude = latitude,
        longitude = longitude)