package com.allclearweather.sensorlibrary

import com.allclearweather.sensorlibrary.models.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class WeatherApi {

    companion object {

        @Throws(IOException::class)
        fun sendPressure(callback: Callback, pressure: Pressure, installId: String, deviceId: String): Call? {
            val client = OkHttpClient()

            InternalConfig.log("sending pressure ${pressure.observationVal} at ${pressure.latitudeVal}, ${pressure.longitudeVal} time ${pressure.timeVal}")
            val JSON = MediaType.parse("application/json; charset=utf-8")

            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", pressure.latitudeVal)
                jsonCondition.put("longitude", pressure.longitudeVal)
                jsonCondition.put("install_id", installId)
                jsonCondition.put("device_id", deviceId)
                jsonCondition.put("time", pressure.timeVal)
                jsonCondition.put("pressureval", pressure.observationVal)
                json = jsonCondition.toString()
                val body = RequestBody.create(JSON, json)

                val request = Request.Builder()
                        .url(InternalConfig.API_URL + "pressure/")
                        .post(body)
                        .build()

                val call = client.newCall(request)
                call.enqueue(callback)
                return call

            } catch (jsone: JSONException) {
                jsone.printStackTrace()
            }

            return null

        }

        @Throws(IOException::class)
        fun sendHumidity(callback: Callback, humidity: Humidity, installId: String, deviceId: String): Call? {
            val client = OkHttpClient()

            val JSON = MediaType.parse("application/json; charset=utf-8")
            InternalConfig.log("sending pressure ${humidity.observationVal} at ${humidity.latitudeVal}, ${humidity.longitudeVal} time ${humidity.timeVal}")
            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", humidity.latitudeVal)
                jsonCondition.put("longitude", humidity.longitudeVal)
                jsonCondition.put("install_id", installId)
                jsonCondition.put("device_id", deviceId)
                jsonCondition.put("time", humidity.timeVal)
                jsonCondition.put("humidityval", humidity.observationVal)
                json = jsonCondition.toString()
                val body = RequestBody.create(JSON, json)

                val request = Request.Builder()
                        .url(InternalConfig.API_URL + "humidity/")
                        .post(body)
                        .build()

                val call = client.newCall(request)
                call.enqueue(callback)
                return call

            } catch (jsone: JSONException) {
                jsone.printStackTrace()
            }

            return null

        }

        @Throws(IOException::class)
        fun sendTemperature(callback: Callback, temperature: Temperature, installId: String, deviceId: String): Call? {
            val client = OkHttpClient()

            val JSON = MediaType.parse("application/json; charset=utf-8")

            InternalConfig.log("sending temperature ${temperature.observationVal} at ${temperature.latitudeVal}, ${temperature.longitudeVal} time ${temperature.timeVal}")
            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", temperature.latitudeVal)
                jsonCondition.put("longitude", temperature.longitudeVal)
                jsonCondition.put("install_id", installId)
                jsonCondition.put("device_id", deviceId)
                jsonCondition.put("time", temperature.timeVal)
                jsonCondition.put("temperatureval", temperature.observationVal)
                json = jsonCondition.toString()
                val body = RequestBody.create(JSON, json)

                val request = Request.Builder()
                        .url(InternalConfig.API_URL + "temperature/")
                        .post(body)
                        .build()

                val call = client.newCall(request)
                call.enqueue(callback)
                return call

            } catch (jsone: JSONException) {
                jsone.printStackTrace()
            }

            return null

        }

        @Throws(IOException::class)
        fun sendLight(callback: Callback, light: Light, installId: String, deviceId: String): Call? {
            val client = OkHttpClient()

            val JSON = MediaType.parse("application/json; charset=utf-8")

            InternalConfig.log("sending pressure ${light.observationVal} at ${light.latitudeVal}, ${light.longitudeVal} time ${light.timeVal}")
            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", light.latitudeVal)
                jsonCondition.put("longitude", light.longitudeVal)
                jsonCondition.put("install_id", installId)
                jsonCondition.put("device_id", deviceId)
                jsonCondition.put("time", light.timeVal)
                jsonCondition.put("lightval", light.observationVal)
                json = jsonCondition.toString()
                val body = RequestBody.create(JSON, json)

                val request = Request.Builder()
                        .url(InternalConfig.API_URL + "light/")
                        .post(body)
                        .build()

                val call = client.newCall(request)
                call.enqueue(callback)
                return call

            } catch (jsone: JSONException) {
                jsone.printStackTrace()
            }

            return null

        }


    }
}