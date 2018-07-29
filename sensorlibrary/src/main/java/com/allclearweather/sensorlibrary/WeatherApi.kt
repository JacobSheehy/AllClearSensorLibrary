package com.allclearweather.sensorlibrary

import com.allclearweather.sensorlibrary.InternalConfig.Companion.log
import com.allclearweather.sensorlibrary.models.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class WeatherApi {

    companion object {

        fun roundAvoid(value: Double, places: Int): Double {
            val scale = Math.pow(10.0, places.toDouble())
            return Math.round(value * scale) / scale
        }

        @Throws(IOException::class)
        fun sendPressure(callback: Callback, pressure: Pressure, installId: String, deviceId: String): Call? {
            val client = OkHttpClient()

            InternalConfig.log("sending pressure ${pressure.observationVal} at ${pressure.latitudeVal}, ${pressure.longitudeVal} time ${pressure.timeVal}")
            val JSON = MediaType.parse("application/json; charset=utf-8")

            if(pressure.latitudeVal==0.0) {
                log("not sending pressure, latitude is 0")
                return null
            }

            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", roundAvoid(pressure.latitudeVal, 2))
                jsonCondition.put("longitude", roundAvoid(pressure.longitudeVal,2))
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

            if(humidity.latitudeVal==0.0) {
                log("not sending humidity, latitude is 0")
                return null
            }

            InternalConfig.log("sending humidity ${humidity.observationVal} at ${humidity.latitudeVal}, ${humidity.longitudeVal} time ${humidity.timeVal}")
            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", roundAvoid(humidity.latitudeVal,2))
                jsonCondition.put("longitude", roundAvoid(humidity.longitudeVal,2))
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

            if(temperature.latitudeVal==0.0) {
                log("not sending temperature, latitude is 0")
                return null
            }

            InternalConfig.log("sending temperature ${temperature.observationVal} at ${temperature.latitudeVal}, ${temperature.longitudeVal} time ${temperature.timeVal}")
            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", roundAvoid(temperature.latitudeVal,2))
                jsonCondition.put("longitude", roundAvoid(temperature.longitudeVal,2))
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

            if(light.latitudeVal==0.0) {
                log("not sending light, latitude is 0")
                return null
            }

            InternalConfig.log("sending light ${light.observationVal} at ${light.latitudeVal}, ${light.longitudeVal} time ${light.timeVal}")
            var json = ""
            try {

                val jsonCondition = JSONObject()
                jsonCondition.put("latitude", roundAvoid(light.latitudeVal,2))
                jsonCondition.put("longitude", roundAvoid(light.longitudeVal,2))
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