package com.allclearweather.sensorlibrary.util

import android.content.Context
import com.allclearweather.sensorlibrary.InternalConfig
import java.io.File

/**
 * Save and read files to store simple csv atmosphere data. Files are private to the app
 * but can be shared through reading the file and sharing an intent
 */
class FileUtil {

    companion object {

        val measurementBufferMax = 1500

        fun log(text: String) {
            println(text)
        }

        fun fileExists(context: Context, filename: String): Boolean {
            val file = context.getFileStreamPath(filename)
            return !(file == null || !file.exists())
        }

        fun cleanOldFile(context: Context, fileName: String) {
            log("running cleanoldfile");
            if(!fileExists(context, fileName)) {
                InternalConfig.log("$fileName does not exist for cleanOldFile")
                return
            }

            var newFile = ""
            val fileContents = readFile(context, fileName)
            var sensorData = fileContents.lines()
            log("${sensorData.size} lines read from file contents")
            if(sensorData.size> measurementBufferMax) {
                sensorData = sensorData.subList(Math.min(10, sensorData.size), sensorData.size)
                log("${sensorData.size} new lines formed")
                for (sensorD in sensorData) {
                    if(sensorD.length>2) {
                        newFile = "$newFile$sensorD\n"
                    }
                }
                saveFileOverwrite(context, fileName, newFile)
            } else {
                println("${sensorData.size} is not more than $measurementBufferMax so not overwriting file");
            }
        }

        fun saveFile(context: Context, fileName: String, fileContents: String) {
            log("sensorlibrary save file")
            context.openFileOutput(fileName, Context.MODE_APPEND).use {
                it.write(fileContents.toByteArray())
            }
        }

        fun saveFileOverwrite(context: Context, fileName: String, fileContents: String) {
            log("sensorlibrary running savefileoverwrite");
            val file = context.getFileStreamPath(fileName)
            file.delete()
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(fileContents.toByteArray())
            }
        }


        fun readFile(context: Context, fileName: String) : String {
            log("sensorlibrary running readfile")
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