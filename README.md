# AllClearSensorLibrary

This is an Android library written in Kotlin for accessing sensors in the foreground. The project is currently a Work-In-Progress and is not yet ready for use in production.

Key features
------------

This sensor library shows an ongoing notification when started, and periodically accesses environmental sensors on Android devices. Currently supported are barometers, hygrometers and thermometers. Data is logged to private CSV files and can be shared via Intent.

Usage
-----

To use this library, download it and make it in Android Studio. The build folder's .aar file can be copied into another Android project's libs folder and included in build.gradle like this: compile files('libs/sensorlibrary-debug.aar'). Other methods of including it in a host app are available.

To start the service, use an Intent, for example in onReceive of BroadcastReceiver:

````

val newIntent = Intent(context, SensorForegroundService::class.java)
context?.startService(newIntent)

````

To stop the service, pass it a stop parameter:

````

val newIntent = Intent(context, SensorForegroundService::class.java)
intent.newIntent.putExtra("stop","")
context?.startService(newIntent)

````

To access data from the service, pass it a view parameter and it will start a Share intent with the data in the content:

````

val newIntent = Intent(context, SensorForegroundService::class.java)
intent.newIntent.putExtra("viewPressureData","")
context?.startService(newIntent)

````
