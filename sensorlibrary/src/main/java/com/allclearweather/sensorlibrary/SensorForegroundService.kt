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
import com.allclearweather.sensorlibrary.api.SensorApi

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
import kotlin.collections.ArrayList
import okhttp3.Response
import java.io.IOException


/**
 * This is a foreground service that accesses sensors and location
 * to log local environmental sensor data on Android devices
 */
class SensorForegroundService : Service(), SensorEventListener, GoogleApiClient.ConnectionCallbacks,
                                                                GoogleApiClient.OnConnectionFailedListener,
                                                                com.google.android.gms.location.LocationListener {

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

        val fusedLocationProviderClient : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
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

    private fun startLocationUpdates() {
        InternalConfig.log("sensorforegroundservice start location updates")
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(SensorControls.UPDATE_INTERVAL)
                .setFastestInterval(SensorControls.FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this)
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = false
        createNotificationChannel()

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

        preferences?.let {
            temperaturePref = it.getString("tempPref","c")
            pressurePref = it.getString("prefPressure","mb")
            sensorsActive = it.getBoolean("sensorsActive", false)
        }

    }

    private fun stopLocationUpdates() {
        mGoogleApiClient?.let {
            if (it.isConnected) {
                it.disconnect()
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        stopLocationUpdates()
        super.onDestroy()

    }

    private fun handleStopSelfFromIntent(intent: Intent?) : Int {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        InternalConfig.log("onstartcommand of sensorforegroundservice")
        preferences?.let {
            sensorsActive = it.getBoolean("sensorsActive", false)
        }

        if((intent?.extras?.get("stop") !=null) || (!sensorsActive))  {
            return handleStopSelfFromIntent(intent)
        }

        intent?.let { startServiceIntent ->
            when {
                startServiceIntent.extras?.get("getPressureData") !=null -> {
                    InternalConfig.log("getPressureData")
                    val fileContents = FileUtil.readFile(applicationContext, "pressure.csv")
                    val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
                    sendDataIntent.putExtra("dataSetPressure", fileContents)
                    sendBroadcast(sendDataIntent)
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("getTemperatureData") !=null -> {
                    InternalConfig.log("getTemperatureData")
                    val fileContents = FileUtil.readFile(applicationContext, "temperature.csv")
                    val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
                    sendDataIntent.putExtra("dataSetTemperature", fileContents)
                    sendBroadcast(sendDataIntent)
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("getHumidityData") !=null -> {
                    InternalConfig.log("getHumidityData")
                    val fileContents = FileUtil.readFile(applicationContext, "humidity.csv")
                    val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
                    sendDataIntent.putExtra("dataSetHumidity", fileContents)
                    sendBroadcast(sendDataIntent)
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("getLightData") !=null -> {
                    InternalConfig.log("getLightData")
                    val fileContents = FileUtil.readFile(applicationContext, "light.csv")
                    val sendDataIntent = Intent("com.allclearweather.android.ACTION_SENSOR_DATA")
                    sendDataIntent.putExtra("dataSetLight", fileContents)
                    sendBroadcast(sendDataIntent)
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("viewPressureData") !=null -> {
                    val fileContents = FileUtil.readFile(applicationContext, "pressure.csv")
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.devicePressureData))
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
                    sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.viewPressureData)))
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("viewHumidityData") !=null -> {
                    val fileContents = FileUtil.readFile(applicationContext, "humidity.csv")
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.deviceHumidityData))
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
                    sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.viewHumidityData)))
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("viewTemperatureData") !=null -> {
                    val fileContents = FileUtil.readFile(applicationContext, "temperature.csv")
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.deviceTemperatureData))
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
                    sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.viewTemperatureData)))
                    return START_NOT_STICKY
                }
                startServiceIntent.extras?.get("viewLightData") !=null -> {
                    val fileContents = FileUtil.readFile(applicationContext, "light.csv")
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.deviceLightData))
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
                    sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.viewLightData)))
                    return START_NOT_STICKY
                } else -> {
                    processServiceStartForDataCollection()
                    return START_STICKY
                }
            }
        }

        stopForeground(true)



        return START_NOT_STICKY
    }

    private fun processServiceStartForDataCollection() {
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

        prepareAndSendNotification()

        startForegroundAndSensorData()
    }

    private fun startForegroundAndSensorData() {
        try {
            startForeground(1, notification)
            isRunning = true

            checkAndUpdateLocation()

            startProcessingSensorData()
        } catch (e: Exception) {
            if (InternalConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    private fun prepareAndSendNotification() {
        val receiverAction = applicationContext.packageName + ".SERVICE_FOREGROUND_NOTIFICATION"
        InternalConfig.log("sensorforegroundservice receiveraction=$receiverAction")
        // No need for Class definition in the constructor.
        val intentAction = Intent()
        // Set the unique action.
        intentAction.action = receiverAction
        // Set the application package name on the Intent, so only the application
        // will have this Intent broadcasted, thus making it “explicit" and secure.
        intentAction.`package` = applicationContext.packageName

        intentAction.putExtra("fromSensorNotification", true)
        val pendingIntent = PendingIntent.getBroadcast(this, notificationRequestCode, intentAction, PendingIntent.FLAG_UPDATE_CURRENT)

        val messageContent = prepareMessageContent()

        notificationBuilder = NotificationCompat.Builder(this, channelName)
                .setSmallIcon(R.drawable.ic_router_24dp)
                .setContentText(messageContent)
                .setContentTitle(getString(R.string.notificationSensorDataTitle))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)

        notification = notificationBuilder?.build()

        notificationManager?.notify(1, notification)
    }

    private fun prepareMessageContent() : String {
        var messageContent = ""
        val df = DecimalFormat("##.##")

        if (pressureValues.size > 0) {
            if (pressurePref == "mb") {
                val pressureVar = pressureValues[pressureValues.size - 1].observationVal
                messageContent = messageContent.plus(df.format(pressureVar) + " mb")
            } else if (pressurePref == "hg") {
                messageContent = messageContent.plus(df.format(WeatherUnits.convertMbToHg(pressureValues[pressureValues.size - 1].observationVal)) + " hg")
            }
            InternalConfig.log("adding pressure to message content")
        }

        messageContent = messageContent.plus("\n")

        if (temperatureValues.size > 0) {

            if (temperaturePref == "c") {
                val temperatureVar = temperatureValues[temperatureValues.size - 1].observationVal
                messageContent = messageContent.plus(df.format(temperatureVar) + " °C")
            } else if (pressurePref == "f") {
                messageContent = messageContent.plus(df.format(WeatherUnits.convertMbToHg(temperatureValues[temperatureValues.size - 1].observationVal)) + " °F")

            }
            InternalConfig.log("adding temperature data to message content")
        }
        messageContent = messageContent.plus("\n")

        if (humidityValues.size > 0) {
            messageContent = messageContent.plus(df.format(WeatherUnits.convertMbToHg(humidityValues[humidityValues.size - 1].observationVal)) + " %")
            InternalConfig.log("adding humidity data to message content")
        }

        messageContent = messageContent.plus("\n")

        if (lightValues.size > 0) {
            messageContent = messageContent.plus(df.format(lightValues[lightValues.size - 1].observationVal) + " lx")
            InternalConfig.log("adding light data to message content")
        }


        InternalConfig.log("isRunning=$isRunning, starting foreground, messagedata = $messageContent")

        if (messageContent.trim() == "") {
            messageContent = getString(R.string.gatheringData)
        }
        return messageContent
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
                DataRecorder.recordTemperatureValues(event,
                        latitude, longitude, applicationContext, temperatureValues)
                SensorControls.stopSensorListener(mSensorManager, mTemperature, this)
            }
            event?.sensor?.type==Sensor.TYPE_RELATIVE_HUMIDITY -> {
                DataRecorder.recordHumidityValues(event,
                        latitude, longitude, applicationContext, humidityValues)
                SensorControls.stopSensorListener(mSensorManager, mHumidity, this)

            }
            event?.sensor?.type==Sensor.TYPE_PRESSURE -> {
                DataRecorder.recordPressureValues(event,
                        latitude, longitude, applicationContext, pressureValues)
                SensorControls.stopSensorListener(mSensorManager, mPressure, this)
            }
            event?.sensor?.type==Sensor.TYPE_LIGHT-> {
                DataRecorder.recordLightValues(event,
                        latitude, longitude, applicationContext, lightValues)
                SensorControls.stopSensorListener(mSensorManager, mLight, this)
            }
        }
    }

    private fun restartSelf() {
        InternalConfig.log("restarting sensor service in 15m delay")
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis()+(1000*60*15),
                alarmPending)
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
            val humid = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
            hasHumiditySensor = humid != null

            val temp = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            hasTemperatureSensor = temp != null

            val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            hasBarometer = pressure != null

            val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            hasLightSensor = light != null
        }

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if(hasBarometer) {
            mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            SensorControls.startSensorListener(mSensorManager, mPressure, this)
        }

        if(hasHumiditySensor) {
            mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
            SensorControls.startSensorListener(mSensorManager, mHumidity, this)
        }

        if(hasTemperatureSensor) {
            mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            SensorControls.startSensorListener(mSensorManager, mTemperature, this)
        }

        if(hasLightSensor) {
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            SensorControls.startSensorListener(mSensorManager, mLight, this)
        }

        restartSelfThread()
    }


    private fun restartSelfThread() {
        Handler().post{
            restartSelf()
        }
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
