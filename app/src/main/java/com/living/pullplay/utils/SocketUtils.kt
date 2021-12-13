package com.living.pullplay.utils

import android.content.Context
import android.net.wifi.WifiManager

class SocketUtils {
    companion object {

        const val LISTENING_PORT = 9621

        fun getHostIp(context: Context): String? {
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.let { mWifiManager ->
                val ipAddress = mWifiManager.connectionInfo.ipAddress
                return if (ipAddress == 0) {
                    null
                } else {
                    (ipAddress and 0xff).toString() + "." + (ipAddress shr 8 and 0xff) + "." + (ipAddress shr 16 and 0xff) + "." + (ipAddress shr 24 and 0xff)
                }
            }
            return null
        }
    }
}