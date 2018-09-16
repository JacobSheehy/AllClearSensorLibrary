package com.allclearweather.sensorlibrary

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder

import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat

import com.allclearweather.sensorlibrary.models.Humidity
import com.allclearweather.sensorlibrary.models.Light
import com.allclearweather.sensorlibrary.models.Pressure
import com.allclearweather.sensorlibrary.models.Temperature
import com.allclearweather.sensorlibrary.util.FileUtil
import com.allclearweather.sensorlibrary.util.Installation
import com.allclearweather.sensorlibrary.util.WeatherUnits
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import okhttp3.Call
import okhttp3.Callback
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.Internal
import java.io.IOException


/**
 * This is a foreground service that accesses sensors and location
 * to log local environmental sensor data on Android devices
 */
class SensorForegroundService : Service() , SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private var notificationManager: NotificationManager? = null
    private var isRunning = false

    private val notificationRequestCode = 0

    private val channelName = "All Clear - Sensor Data"

    private var hasBarometer = false
    private var hasHumiditySensor = false
    private var hasTemperatureSensor = false
    private var hasLightSensor = false

    private lateinit var mSensorManager: SensorManager
    private var mPressure: Sensor? = null
    private var mHumidity: Sensor? = null
    private var mTemperature: Sensor? = null
    private var mLight: Sensor? = null

    private var pressureValues : ArrayList<Pressure> = ArrayList()
    private var temperatureValues : ArrayList<Temperature> = ArrayList()
    private var humidityValues : ArrayList<Humidity> = ArrayList()
    private var lightValues : ArrayList<Light> = ArrayList()

    var notificationBuilder : NotificationCompat.Builder? = null
    var notification : Notification? = null

    private var pressurePref = "mb"
    private var temperaturePref = "c"

    private var preferences : SharedPreferences? = null
    private var editor : SharedPreferences.Editor? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var alarmManager : AlarmManager

    private var latitude : Double = 0.0
    private var longitude : Double = 0.0

    private var alarmPending : PendingIntent? = null

    private var sensorsActive = false




    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationManager: LocationManager? = null
    lateinit var mLocation: Location
    private var mLocationRequest: LocationRequest? = null
    private val listener: com.google.android.gms.location.LocationListener? = null
    private val UPDATE_INTERVAL = (2 * 1000).toLong()  /* 10 secs */
    private val FASTEST_INTERVAL: Long = 2000 /* 2 sec */

    lateinit var locationManager: LocationManager

    override fun onConnectionSuspended(p0: Int) {

        InternalConfig.log("on connection suspended")
        mGoogleApiClient?.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        InternalConfig.log("on connection failed")
    }

    override fun onLocationChanged(location: Location) {
        var msg = "Updated Location: Latitude " + location.longitude.toString() + location.longitude;
        latitude = location.latitude
        longitude = location.longitude

    }

    override fun onConnected(p0: Bundle?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        startLocationUpdates()

        var fusedLocationProviderClient : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient .lastLocation
                .addOnSuccessListener(this, OnSuccessListener<Location> { location ->

                    if (location != null) {
                        InternalConfig.log("got last location")
                        mLocation = location;
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                })
    }


    protected fun startLocationUpdates() {
        InternalConfig.log("sensorforegroundservice start location updates")
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this)
    }


    override fun onCreate() {
        super.onCreate()
        val context = this as Context
        isRunning = false
        createNotificationChannel()

        temperatureValues = ArrayList()
        pressureValues = ArrayList()
        humidityValues = ArrayList()
        lightValues = ArrayList()

        setPrefs()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmPending =  PendingIntent.getBroadcast(
                applicationContext,
                0,
                Intent(applicationContext, StartDataReceiver::class.java).apply {},
                PendingIntent.FLAG_CANCEL_CURRENT)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        checkAndUpdateLocation()


    }

    private fun checkAndUpdateLocation() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            InternalConfig.log("sensorforegroundservice checking location")
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            latitude = location.latitude
                            longitude = location.longitude
                            InternalConfig.log("sensorforegroundservice got location $latitude")
                        } else {
                            InternalConfig.log("sensorforegroundservice null location")
                        }
                    }
        }
    }

    private fun setPrefs() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        editor = preferences?.edit()

        temperaturePref = preferences!!.getString("tempPref","c")
        pressurePref = preferences!!.getString("prefPressure","mb")
        sensorsActive = preferences!!.getBoolean("sensorsActive", false)
    }

    private fun stopLocationUpdates() {
        if(mGoogleApiClient!=null) {
            if (mGoogleApiClient!!.isConnected) {
                mGoogleApiClient?.disconnect()
            }
        }

    }

    override fun onDestroy() {
        isRunning = false
        stopLocationUpdates()
        super.onDestroy()

    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        InternalConfig.log("onstartcommand of sensorforegroundservice")
        sensorsActive = preferences!!.getBoolean("sensorsActive", false)
        if((intent?.extras?.get("stop") !=null) || (!sensorsActive))  {
            isRunning = false
            notificationManager?.cancel(1)
            stopForeground(true)
            InternalConfig.log("onstartcommand stopping running service sensorsactive: $sensorsActive, stop= " + intent?.extras?.get("stop"))

            editor = preferences?.edit()
            editor?.putBoolean("sensorsActive", false)
            editor?.apply()
            this.stopSelf()
            return START_NOT_STICKY
        }



        if(intent?.extras?.get("getPressureData") !=null)  {
            InternalConfig.log("getPressureData")
            val fileContents = FileUtil.readFile(applicationContext, "pressure.csv")
            val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
            sendDataIntent.putExtra("dataSetPressure", fileContents)
            sendBroadcast(sendDataIntent)
            return START_NOT_STICKY
        }



        if(intent?.extras?.get("getTemperatureData") !=null)  {
            InternalConfig.log("getTemperatureData")
            val fileContents = FileUtil.readFile(applicationContext, "temperature.csv")
            val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
            sendDataIntent.putExtra("dataSetTemperature", fileContents)
            sendBroadcast(sendDataIntent)
            return START_NOT_STICKY
        }



        if(intent?.extras?.get("getHumidityData") !=null)  {
            InternalConfig.log("getHumidityData")
            val fileContents = FileUtil.readFile(applicationContext, "humidity.csv")
            val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
            sendDataIntent.putExtra("dataSetHumidity", fileContents)
            sendBroadcast(sendDataIntent)
            return START_NOT_STICKY
        }


        if(intent?.extras?.get("getLightData") !=null)  {
            InternalConfig.log("getLightData")
            val fileContents = FileUtil.readFile(applicationContext, "light.csv")
            val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
            sendDataIntent.putExtra("dataSetLight", fileContents)
            sendBroadcast(sendDataIntent)
            return START_NOT_STICKY
        }

        if(intent?.extras?.get("viewPressureData") !=null)  {
            val fileContents = FileUtil.readFile(applicationContext, "pressure.csv")
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "All Clear - Device Pressure Data")
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(Intent.createChooser(sharingIntent, "View Pressure Data (.csv)"))
            return START_NOT_STICKY
        }

        if(intent?.extras?.get("viewHumidityData") !=null)  {
            val fileContents = FileUtil.readFile(applicationContext, "humidity.csv")
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "All Clear - Device Humidity Data")
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(Intent.createChooser(sharingIntent, "View Humidity Data (.csv)"))
            return START_NOT_STICKY
        }

        if(intent?.extras?.get("viewTemperatureData") !=null)  {
            val fileContents = FileUtil.readFile(applicationContext, "temperature.csv")
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "All Clear - Device Temperature Data")
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(Intent.createChooser(sharingIntent, "View Temperature Data (.csv)"))
            return START_NOT_STICKY
        }


        if(intent?.extras?.get("viewLightData") !=null)  {
            val fileContents = FileUtil.readFile(applicationContext, "light.csv")
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "All Clear - Device Light Data")
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(Intent.createChooser(sharingIntent, "View Temperature Light (.csv)"))
            return START_NOT_STICKY
        }


        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        mLocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        checkLocation()


        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPending)

        InternalConfig.log("onstartcommand of sensorforegroundservice, is running? $isRunning")
        //var notificationIntent = Intent("com.allclearweather.allclearsensorlibrary")

        val receiverAction = applicationContext.packageName + ".SERVICE_FOREGROUND_NOTIFICATION"
        InternalConfig.log("sensorforegroundservice receiveraction=$receiverAction")
        // No need for Class definition in the constructor.
        val intent = Intent()
        // Set the unique action.
        intent.action = receiverAction
        // Set the application package name on the Intent, so only the application
        // will have this Intent broadcasted, thus making it “explicit" and secure.
        intent.`package` = applicationContext.packageName

        intent.putExtra("fromSensorNotification",true)
        var pendingIntent = PendingIntent.getBroadcast(this,  notificationRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        var messageContent = ""

        if(pressureValues.size>0) {
            var df = DecimalFormat("##.##")

            if(pressurePref == "mb") {
                var pressureVar = pressureValues[pressureValues.size-1].observationVal
                messageContent = messageContent.plus(df.format(pressureVar) + " mb")
            } else if (pressurePref == "hg") {
                messageContent = messageContent.plus(df.format(WeatherUnits.convertMbToHg(pressureValues[pressureValues.size-1].observationVal)) + " hg")
            }
            InternalConfig.log("adding psea to message content")
        }

        messageContent = messageContent.plus("\n")

        if(temperatureValues.size>0) {
            var df = DecimalFormat("##.##")

            if(temperaturePref == "c") {
                var temperatureVar = temperatureValues[temperatureValues.size-1].observationVal
                messageContent = messageContent.plus(df.format(temperatureVar) + " °C")
            } else if (pressurePref == "f") {
                messageContent = messageContent.plus(df.format(WeatherUnits.convertMbToHg(temperatureValues[temperatureValues.size-1].observationVal)) + " °F")

            }
            InternalConfig.log("adding temperature data to message content")
        }

        messageContent = messageContent.plus("\n")

        if(humidityValues.size>0) {
            var df = DecimalFormat("##.##")
            messageContent = messageContent.plus(df.format(WeatherUnits.convertMbToHg(humidityValues[humidityValues.size-1].observationVal)) + " %")

            InternalConfig.log("adding humidity data to message content")
        }

        messageContent = messageContent.plus("\n")

        if(lightValues.size>0) {
            var df = DecimalFormat("##.##")
            messageContent = messageContent.plus(df.format(lightValues[lightValues.size-1].observationVal) + " lx")

            InternalConfig.log("adding light data to message content")
        }


        InternalConfig.log("isRunning=$isRunning, starting foreground, messagedata = $messageContent")

        if(messageContent.trim() == ""){
            messageContent = "Gathering data, check back soon!"
        }

        notificationBuilder = NotificationCompat.Builder(this, channelName)
                .setSmallIcon(R.drawable.ic_router_24dp)
                .setContentText(messageContent)
                .setContentTitle("All Clear sensor data")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)



        notification = notificationBuilder?.build()
        notificationManager?.notify(1, notification)



        try {
            startForeground(1, notification)
            isRunning = true

            checkAndUpdateLocation()

            startProcessingSensorData()
        } catch(e: Exception) {
            if(InternalConfig.DEBUG) {
                e.printStackTrace()
            }
        }




        return START_STICKY
    }




    private fun checkLocation(): Boolean {
        if(!isLocationEnabled()) {
            InternalConfig.log("sensorforeground service location is not enabled")
        }
        return isLocationEnabled()
    }

    private fun isLocationEnabled(): Boolean {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // TODO
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when {
            event?.sensor?.type==Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                recordTemperatureValues(event)
                stopTemperatureListener()

            }
            event?.sensor?.type==Sensor.TYPE_RELATIVE_HUMIDITY -> {
                recordHumidityValues(event)
                stopHumidityListener()

            }
            event?.sensor?.type==Sensor.TYPE_PRESSURE -> {
                recordPressureValues(event)
                stopPressureListener()
            }
            event?.sensor?.type==Sensor.TYPE_LIGHT-> {
                recordLightValues(event)
                stopLightListener()
            }
        }
    }

    private fun restartSelf() {
        InternalConfig.log("restarting sensor service in 10m delay")
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis()+(1000*60*10),
                alarmPending)
    }

    private fun sendHumidityToServer(humidity: Humidity) {
        try {
            WeatherApi.sendHumidity(object : Callback {
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
            }, humidity, Installation.id(applicationContext), Installation.getID(applicationContext))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

    }


    private fun sendPressureToServer(pressure: Pressure) {
        try {
            WeatherApi.sendPressure(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    InternalConfig.log("failure to send pressure data to server")
                    if (InternalConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        // sent well
                        InternalConfig.log("sent pressure successfully");
                    } else {
                        InternalConfig.log(response.message())
                    }
                }
            }, pressure, Installation.id(applicationContext), Installation.getID(applicationContext))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

    }

    private fun sendTemperatureToServer(temperature: Temperature) {
        try {
            WeatherApi.sendTemperature(object : Callback {
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
            },temperature, Installation.id(applicationContext), Installation.getID(applicationContext))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

    }

    private fun sendLightToServer(light: Light) {
        try {
            WeatherApi.sendLight(object : Callback {
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
            },light, Installation.id(applicationContext), Installation.getID(applicationContext))
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

    }

    private fun recordHumidityValues(event: SensorEvent) {
        if(latitude ==0.0) {
            InternalConfig.log("not recording humidity, latitude is 0");
            return
        }

        val eventVal = event.values[0]
        val newHumidity = Humidity(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newHumidity.toCSV() + "\n"
        humidityValues.add(newHumidity)
        sendHumidityToServer(newHumidity)
        FileUtil.cleanOldFile(applicationContext, "humidity.csv")
        FileUtil.saveFile(applicationContext, "humidity.csv",newData)
    }

    private fun recordPressureValues(event: SensorEvent) {
        if(latitude ==0.0) {
            InternalConfig.log("not recording pressure, latitude is 0");
            return
        }
        val eventVal = event.values[0]
        val newPressure = Pressure(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newPressure.toCSV() + "\n"
        InternalConfig.log("pressure data: $newData")
        pressureValues.add(newPressure)
        sendPressureToServer(newPressure)
        FileUtil.cleanOldFile(applicationContext, "pressure.csv")
        FileUtil.saveFile(applicationContext, "pressure.csv",newData)
    }

    private fun recordTemperatureValues(event: SensorEvent) {
        val eventVal = event.values[0]
        val newTemperature = Temperature(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newTemperature.toCSV() + "\n"
        temperatureValues.add(newTemperature)
        sendTemperatureToServer(newTemperature)
        FileUtil.cleanOldFile(applicationContext, "temperature.csv")
        FileUtil.saveFile(applicationContext, "temperature.csv",newData)
    }


    private fun recordLightValues(event: SensorEvent) {
        if(latitude ==0.0) {
            InternalConfig.log("not recording light, latitude is 0");
            return
        }
        val eventVal = event.values[0]
        val newLight = Light(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newLight.toCSV() + "\n"
        lightValues.add(newLight)
        sendLightToServer(newLight)
        FileUtil.cleanOldFile(applicationContext, "light.csv")
        FileUtil.saveFile(applicationContext, "light.csv",newData)
    }


    private fun startProcessingSensorData() {
        InternalConfig.log("start processing sensor data")
        val manager = packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hasBarometer = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER)
            hasHumiditySensor = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_RELATIVE_HUMIDITY)
            hasTemperatureSensor = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_AMBIENT_TEMPERATURE)
            hasLightSensor = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT)
        } else {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var humid = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

            hasHumiditySensor = humid != null

            var temp = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            hasTemperatureSensor = temp != null

            var pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            hasBarometer = pressure != null

            var light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            hasLightSensor = light != null
        }

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if(hasBarometer) {
            mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            startPressureListener()
        }

        if(hasHumiditySensor) {
            mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
            startHumidityListener()
        }

        if(hasTemperatureSensor) {
            mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            startTemperatureListener()
        }

        if(hasLightSensor) {
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            startLightListener()
        }

        restartSelfDelayed()
    }


    private fun restartSelfDelayed() {
        Handler().postDelayed({
            //doSomethingHere()
            restartSelf()
        }, 3000)
    }

    private fun startLightListener() {
        InternalConfig.log("startlightsensor")
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)
    }


    private fun startHumidityListener() {
        InternalConfig.log("starthumiditysensor")
        mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startPressureListener() {
        InternalConfig.log("startpressuresensor")
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startTemperatureListener() {
        InternalConfig.log("starttemperaturesensor")
        mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopProcessingSensorData() {
        stopTemperatureListener()
        stopPressureListener()
        stopHumidityListener()
        stopLightListener()
    }

    fun stopLightListener() {
        mSensorManager.unregisterListener(this, mLight)
    }

    fun stopHumidityListener() {
        mSensorManager.unregisterListener(this, mHumidity)
    }

    fun stopTemperatureListener() {
        mSensorManager.unregisterListener(this, mTemperature)
    }

    fun stopPressureListener() {
        mSensorManager.unregisterListener(this, mPressure)
    }


    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = channelName
            val description = "This is the persistent notification for All Clear to collect environmental sensor data for research and forecasting purposes."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelName, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }


    private fun <TResult> Task<TResult>.addOnSuccessListener(sensorForegroundService: SensorForegroundService, onSuccessListener: OnSuccessListener<TResult>) {
        InternalConfig.log("sensorforeground service added on success listener")
        stopLocationUpdates()
    }

}
