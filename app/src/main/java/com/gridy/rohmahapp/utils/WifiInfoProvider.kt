// File: /app/src/main/java/com/gridy/rohmahapp/utils/WifiInfoProvider.kt
package com.gridy.rohmahapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address

data class CapturedNetworkInfo(
    val ssid: String?,
    val bssid: String?,
    val localIp: String?,
    val gatewayIp: String?,
    val subnetPrefix: Int?,
    val transport: String = "WIFI"
)

class WifiInfoProvider(
    private val context: Context
) {

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun capture(): CapturedNetworkInfo? {
        if (!isWifiConnected()) return null

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val linkProperties = cm.getLinkProperties(network)

        val ipv4Link = linkProperties
            ?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address }

        val gateway = linkProperties
            ?.routes
            ?.firstOrNull { it.gateway is Inet4Address }
            ?.gateway
            ?.hostAddress

        val rawSsid = wifiManager.connectionInfo?.ssid
        val ssid = rawSsid?.replace("\"", "")
        val bssid = wifiManager.connectionInfo?.bssid

        return CapturedNetworkInfo(
            ssid = ssid,
            bssid = bssid,
            localIp = ipv4Link?.address?.hostAddress,
            gatewayIp = gateway,
            subnetPrefix = ipv4Link?.prefixLength,
            transport = "WIFI"
        )
    }
}