package com.allclearweather.sensorlibrary

class InternalConfig {
    companion object {
        val API_URL = ""

        const val DEBUG = true

        fun log(text: String) {
            if(DEBUG) {
                println(text)
            }
        }
    }
}