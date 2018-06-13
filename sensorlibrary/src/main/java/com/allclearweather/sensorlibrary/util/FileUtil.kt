package com.allclearweather.sensorlibrary.util

import android.content.Context
import java.io.File
import java.util.ArrayList

/**
 * Save and read files to store simple csv atmosphere data. Files are private to the app
 * but can be shared through reading the file and sharing an intent
 */
class FileUtil {

    companion object {

        fun fileExists(context: Context, filename: String): Boolean {
            val file = context.getFileStreamPath(filename)
            return !(file == null || !file.exists())
        }

        fun cleanOldFile(context: Context, fileName: String) {
            if(!fileExists(context, fileName)) {
                println("$fileName does not exist for cleanFile")
                return
            }
            val fileContents = FileUtil.readFile(context, fileName)
            var sensorData = fileContents.split("\n".toRegex())
            println("cleanOldFile starting on $fileName with ${sensorData.size} items")
            if(sensorData.size>10) {
                sensorData = sensorData.subList(Math.min(10, sensorData.size), sensorData.size)
            }

            println("cleanOldFile cut down to ${sensorData.size} items")

            var newFile = sensorData.joinToString { "\n" }


            saveFile(context, fileName, newFile)
        }

        fun saveFile(context: Context, fileName: String, fileContents: String) {
            context.openFileOutput(fileName, Context.MODE_APPEND).use {
                it.write(fileContents.toByteArray())
            }
        }

        fun readFile(context: Context, fileName: String) : String {
            var content = ""
            context.openFileInput(fileName).use {
                val input = ByteArray(it.available())
                while (it.read(input) !== -1) {
                    content += String(input)
                }
            }
            return content
        }
    }
}