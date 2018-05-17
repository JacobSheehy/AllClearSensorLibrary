package com.allclearweather.sensorlibrary.util

/**
 * Convert between units necessary for notification display for the foreground service
 */
class WeatherUnits {
    companion object {
        fun convertHgToMb(hg: Double): Double {
            return (hg * 33.8639).toInt().toDouble()
        }

        fun convertMbToHg(mb: Double): Double {
            return (mb * 0.02953).toInt().toDouble()
        }
    }
}