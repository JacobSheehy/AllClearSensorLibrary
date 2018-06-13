package com.allclearweather.sensorlibrary.util

import android.content.Context
import java.io.File

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
                println("$fileName does not exist for cleanOldFile")
                return
            }

            var newFile = ""
            val fileContents = readFile(context, fileName)
            var sensorData = fileContents.lines()

            if(sensorData.size>100) {
                sensorData = sensorData.subList(Math.min(10, sensorData.size), sensorData.size)
                for (sensorD in sensorData) {
                    newFile = "$newFile\n$sensorD"
                }
                saveFileOverwrite(context, fileName, newFile)
            }
        }

        fun saveFile(context: Context, fileName: String, fileContents: String) {
            context.openFileOutput(fileName, Context.MODE_APPEND).use {
                it.write(fileContents.toByteArray())
            }
        }

        fun saveFileOverwrite(context: Context, fileName: String, fileContents: String) {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
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