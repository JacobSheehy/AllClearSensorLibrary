package com.allclearweather.sensorlibrary.models

open class Observation constructor(timeRecorded: Long, observation: Double, latitude: Double, longitude: Double) {

    var observationVal: Double = observation
    var timeVal : Long = timeRecorded
    var latitudeVal : Double = latitude
    var longitudeVal : Double = longitude

    fun toCSV() : String {
        return "$observationVal,$timeVal,$latitudeVal,$longitudeVal"
    }

}