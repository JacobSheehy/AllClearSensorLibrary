package com.allclearweather.sensorlibrary

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import com.allclearweather.sensorlibrary.models.Humidity
import com.allclearweather.sensorlibrary.models.Pressure
import com.allclearweather.sensorlibrary.models.Temperature
import com.allclearweather.sensorlibrary.util.FileUtil
import com.allclearweather.sensorlibrary.util.WeatherUnits
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.DecimalFormat
import kotlin.collections.ArrayList

/**
 * This is a foreground service that accesses sensors and location
 * to log local environmental sensor data on Android devices
 */
class SensorForegroundService : Service() , SensorEventListener {

    private var notificationManager: NotificationManager? = null
    private var isRunning = false

    private val notificationRequestCode = 1000

    private val channelName = "CHANNEL_ALLCLEAR"

    private var hasBarometer = false
    private var hasHumiditySensor = false
    private var hasTemperatureSensor = false

    private lateinit var mSensorManager: SensorManager
    private var mPressure: Sensor? = null
    private var mHumidity: Sensor? = null
    private var mTemperature: Sensor? = null

    private var pressureValues : ArrayList<Pressure> = ArrayList()
    private var temperatureValues : ArrayList<Temperature> = ArrayList()
    private var humidityValues : ArrayList<Humidity> = ArrayList()

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

    override fun onCreate() {
        super.onCreate()
        val context = this as Context
        isRunning = false
        createNotificationChannel()

        temperatureValues = ArrayList()
        pressureValues = ArrayList()
        humidityValues = ArrayList()

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
            println("sensorforegroundservice checking location")
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            latitude = location.latitude
                            longitude = location.longitude
                            println("sensorforegroundservice got location $latitude")
                        } else {
                            println("sensorforegroundservice null location")
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

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onstartcommand of sensorforegroundservice")
        sensorsActive = preferences!!.getBoolean("sensorsActive", false)
        if((intent?.extras?.get("stop") !=null) || (!sensorsActive))  {
            isRunning = false
            notificationManager?.cancel(1)
            stopForeground(true)
            println("onstartcommand stopping running service")
            this.stopSelf()
            return START_NOT_STICKY
        }

        if(intent?.extras?.get("viewPressureData") !=null)  {
            val fileContents = FileUtil.readFile(applicationContext, "pressure.csv")
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "All Clear - Device Pressure Data")
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, fileContents)
            startActivity(Intent.createChooser(sharingIntent, "View Pressure Data (.csv)"))
        }


        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPending)

        println("onstartcommand of sensorforegroundservice, is running? $isRunning")
        var notificationIntent = Intent("com.allclearweather.allclearsensorlibrary.SERVICE_FOREGROUND_NOTIFICATION")
        var pendingIntent = PendingIntent.getActivity(this,  notificationRequestCode, notificationIntent, 0)

        var messageContent = ""

        if(pressureValues.size>0) {
            var df = DecimalFormat("##.##")

            if(pressurePref == "mb") {
                messageContent += pressureValues[0].observationVal as String + " mb"
            } else if (pressurePref == "hg") {
                messageContent += df.format(WeatherUnits.convertMbToHg(pressureValues[0].observationVal)) + " hg"
            }
            println("adding pressure data to message content")
        }

        messageContent+="\n"

        if(temperatureValues.size>0) {
            var df = DecimalFormat("##.##")

            if(temperaturePref == "c") {
                messageContent += temperatureValues[0].observationVal as String + " °C"
            } else if (pressurePref == "f") {
                messageContent += df.format(WeatherUnits.convertMbToHg(temperatureValues[0].observationVal)) + " °F"
            }
            println("adding temperature data to message content")
        }

        messageContent+="\n"

        if(humidityValues.size>0) {
            var df = DecimalFormat("##.##")
            messageContent += df.format(WeatherUnits.convertMbToHg(temperatureValues[0].observationVal)) + " %"
            println("adding humidity data to message content")
        }


        if(!isRunning) {
            println("not running, starting foreground")

            var notificationBuilder = NotificationCompat.Builder(this, channelName)
                    .setSmallIcon(R.drawable.partlycloudy)
                    .setContentText(messageContent)
                    .setContentTitle("All Clear sensor data")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)


            var notification = notificationBuilder.build()
            notificationManager?.notify(1, notification)


            startForeground(1, notification)
            isRunning = true
        } else {
            println("service already running, not starting foreground")
        }

        checkAndUpdateLocation()

        startProcessingSensorData()

        return START_STICKY
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
                stopPressureListener(true)
            }
        }
    }

    private fun restartSelf() {
        println("restarting service in 30s delay")
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis()+(1000*30),
                alarmPending)
    }


    private fun recordHumidityValues(event: SensorEvent) {
        val eventVal = event.values[0]
        val newHumidity = Humidity(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newHumidity.toCSV() + "\n"

        FileUtil.saveFile(applicationContext, "humidity.csv",newData)
    }

    private fun recordPressureValues(event: SensorEvent) {
        val eventVal = event.values[0]
        val newPressure = Pressure(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newPressure.toCSV() + "\n"
        println("pressure data: $newData")
        FileUtil.saveFile(applicationContext, "pressure.csv",newData)
    }

    private fun recordTemperatureValues(event: SensorEvent) {
        val eventVal = event.values[0]
        val newTemperature = Temperature(System.currentTimeMillis(), eventVal.toDouble(), latitude, longitude)
        val newData = newTemperature.toCSV() + "\n"

        FileUtil.saveFile(applicationContext, "temperature.csv",newData)
    }


    private fun startProcessingSensorData() {
        println("start processing sensor data")
        val manager = packageManager
        hasBarometer = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hasHumiditySensor = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_RELATIVE_HUMIDITY)
            hasTemperatureSensor = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_AMBIENT_TEMPERATURE)
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
    }

    private fun startHumidityListener() {
        println("starthumiditysensor")
        mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startPressureListener() {
        println("startpressuresensor")
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startTemperatureListener() {
        println("starttemperaturesensor")
        mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopProcessingSensorData() {
        stopTemperatureListener()
        stopPressureListener(false)
        stopHumidityListener()

    }

    fun stopHumidityListener() {
        mSensorManager.unregisterListener(this, mHumidity)
    }

    fun stopTemperatureListener() {
        mSensorManager.unregisterListener(this, mTemperature)
    }

    fun stopPressureListener(startAgain : Boolean) {
        mSensorManager.unregisterListener(this, mPressure)

        if(startAgain) {
            println("stopping pressure listener; starting again in a minute")
            restartSelf()
        } else {
            println("stopping pressure listener, not starting again")
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
            val description = "ChannelDescription"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelName, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

}