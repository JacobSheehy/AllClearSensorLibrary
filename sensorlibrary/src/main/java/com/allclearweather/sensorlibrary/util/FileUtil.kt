package com.allclearweather.sensorlibrary.util

import android.content.Context

class FileUtil {
    companion object {
        fun saveFile(context: Context, fileName: String, fileContents: String) {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(fileContents.toByteArray())
            }
        }

        fun readFile(context: Context, fileName: String) : String {
            val directory = context.filesDir
            println("kotlin readfile directory $directory and filename $fileName")
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