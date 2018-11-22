package com.allclearweather.sensorlibrary.api

import android.content.Context
import com.allclearweather.sensorlibrary.InternalConfig
import com.allclearweather.sensorlibrary.InternalConfig.Companion.log
import com.allclearweather.sensorlibrary.models.*
import com.allclearweather.sensorlibrary.util.Installation
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Send sensor data to the server.
 */

class SensorApi {

    companion object {

        fun roundAvoid(value: Double, places: Int): Double {
            val scale = Math.pow(10.0, places.toDouble())
            return Math.round(value * scale) / scale
        }

        fun sendPressureToServer(pressure: Pressure, context: Context) {
            try {
                sendPressure(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        log("failure to send pressure data to server")
                        if (InternalConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            // sent well
                            log("sent pressure successfully");
                        } else {
                            log(response.message())
                        }
                    }
                }, pressure, Installation.id(context), Installation.getID(context))
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }

        fun sendTemperatureToServer(temperature: Temperature, context: Context) {
            try {
                sendTemperature(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        InternalConfig.log("failure to send temperature data to server")
                        if (InternalConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            // sent well
                            InternalConfig.log("sent temperature successfully");
                        } else {
                            InternalConfig.log(response.message())
                        }
                    }
                },temperature, Installation.id(context), Installation.getID(context))
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }

        }

        fun sendLightToServer(light: Light, context: Context) {
            try {
                sendLight(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        InternalConfig.log("failure to send light data to server")
                        if (InternalConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            // sent well
                        } else {
                            InternalConfig.log(response.message())
                        }
                    }
                },light, Installation.id(context), Installation.getID(context))
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }

        }

        fun sendHumidityToServer(humidity: Humidity, context: Context) {
            try {
                sendHumidity(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        InternalConfig.log("failure to send humidity data to server")
                        if (InternalConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            InternalConfig.log("sent humidity successfully");
                        } else {
                            InternalConfig.log(response.message())
                        }
                    }
                }, humidity, Installation.id(context), Installation.getID(context))
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
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
                jsonCondition.put("longitude", roundAvoid(pressure.longitudeVal, 2))
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
                jsonCondition.put("latitude", roundAvoid(humidity.latitudeVal, 2))
                jsonCondition.put("longitude", roundAvoid(humidity.longitudeVal, 2))
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
                jsonCondition.put("latitude", roundAvoid(temperature.latitudeVal, 2))
                jsonCondition.put("longitude", roundAvoid(temperature.longitudeVal, 2))
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
                jsonCondition.put("latitude", roundAvoid(light.latitudeVal, 2))
                jsonCondition.put("longitude", roundAvoid(light.longitudeVal, 2))
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