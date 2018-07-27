package com.allclearweather.sensorlibrary

class InternalConfig {
    companion object {
        val API_URL = "https://api.allclearweather.com/"

        const val DEBUG = false

        fun log(text: String) {
            if(DEBUG) {
                println(text)
            }
        }
    }
}