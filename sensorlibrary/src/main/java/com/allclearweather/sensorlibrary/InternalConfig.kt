package com.allclearweather.sensorlibrary

class InternalConfig {
    companion object {
        const val DEBUG = true

        fun log(text: String) {
            if(DEBUG) {
                InternalConfig.log(text)
            }
        }
    }
}