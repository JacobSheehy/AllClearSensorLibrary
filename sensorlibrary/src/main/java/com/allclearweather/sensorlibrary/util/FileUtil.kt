package com.allclearweather.sensorlibrary.util

import android.content.Context

/**
 * Save and read files to store simple csv atmosphere data. Files are private to the app
 * but can be shared through reading the file and sharing an intent
 */
class FileUtil {

    companion object {
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