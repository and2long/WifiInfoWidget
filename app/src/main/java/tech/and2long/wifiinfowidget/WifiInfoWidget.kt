package tech.and2long.wifiinfowidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.RemoteViews
import android.net.wifi.WifiInfo
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.ComponentName
import android.util.Log
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WifiInfoWidget : AppWidgetProvider() {

    companion object {
        private val TAG = "WifiInfoWidget"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate: ")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.wifi_info_widget)

        val (ssid, ip, gateway) = getWifiDetails(context)
        remoteViews.setTextViewText(R.id.text_ip, "IP: $ip")
        remoteViews.setTextViewText(R.id.text_gateway, "Gateway: $gateway")

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 注册 WiFi 状态变化的广播接收器
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(wifiStateReceiver, filter)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 取消注册广播接收器
        try {
            context.unregisterReceiver(wifiStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (appWidgetIds != null) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WifiInfoWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    fun getWifiDetails(context: Context): Triple<String, String, String> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        val wifiInfo = wifiManager.connectionInfo

        // 1. 获取 SSID
        val ssid = if (wifiInfo.ssid != WifiManager.UNKNOWN_SSID) {
            wifiInfo.ssid.trim('"')
        } else {
            "未知 Wi-Fi"
        }

        // 2. 获取 IP 地址
        val ipAddress = if (wifiInfo.ipAddress != 0) {
            val ipBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(wifiInfo.ipAddress).array()
            InetAddress.getByAddress(ipBytes).hostAddress ?: "未知"
        } else {
            "未连接"
        }

        // 3. 获取网关地址
        val gatewayAddress = if (dhcpInfo.gateway != 0) {
            val gatewayBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dhcpInfo.gateway).array()
            InetAddress.getByAddress(gatewayBytes).hostAddress ?: "未知"
        } else {
            "未知"
        }

        return Triple(ssid, ipAddress, gatewayAddress)
    }

}