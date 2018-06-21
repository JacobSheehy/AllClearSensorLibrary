package com.allclearweather.sensorlibrary.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.*

object Installation {
    private var sID: String? = null
    private val INSTALLATION = "INSTALLATION"

    @Synchronized
    fun id(context: Context): String {
        if (sID == null) {
            val installation = File(context.filesDir, INSTALLATION)
            try {
                if (!installation.exists())
                    writeInstallationFile(installation)
                sID = readInstallationFile(installation)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }
        return sID as String
    }

    @Throws(IOException::class)
    private fun readInstallationFile(installation: File): String {
        val f = RandomAccessFile(installation, "r")
        val bytes = ByteArray(f.length().toInt())
        f.readFully(bytes)
        f.close()
        return String(bytes)
    }

    @Throws(IOException::class)
    private fun writeInstallationFile(installation: File) {
        val out = FileOutputStream(installation)
        val id = UUID.randomUUID().toString()
        out.write(id.toByteArray())
        out.close()
    }

    /**
     * Get a unique ID by fetching the phone ID and hashing it
     *
     * @return
     */
    fun getID(context: Context): String {
        try {
            val md = MessageDigest.getInstance("MD5")

            @SuppressLint("HardwareIds") val actual_id = Settings.Secure.getString(context
                    .contentResolver, Settings.Secure.ANDROID_ID)
            val bytes = actual_id.toByteArray()
            val digest = md.digest(bytes)
            val hexString = StringBuffer()
            for (i in digest.indices) {
                hexString.append(Integer.toHexString(0xFF and digest[i].toInt()))
            }
            return hexString.toString()
        } catch (e: Exception) {
            return "--"
        }

    }


}