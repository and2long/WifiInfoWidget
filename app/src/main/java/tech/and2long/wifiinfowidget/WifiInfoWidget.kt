package tech.and2long.wifiinfowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.net.wifi.WifiManager
import android.util.Log
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WifiInfoWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "WifiInfoWidget"
        private const val ACTION_REFRESH = "tech.and2long.wifiinfowidget.ACTION_REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate() called with: appWidgetIds = ${appWidgetIds.toList()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive() called with: intent = $intent")
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(
                        context,
                        WifiInfoWidget::class.java
                    )
                )
            for (appWidgetId in ids) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "updateAppWidget() called with: appWidgetId = $appWidgetId")
        val remoteViews = RemoteViews(context.packageName, R.layout.wifi_info_widget)
        val (ssid, ip, gateway) = getWifiDetails(context)
        remoteViews.setTextViewText(R.id.text_ip, "IP: $ip")
        remoteViews.setTextViewText(R.id.text_gateway, "网关: $gateway")

        // 设置刷新按钮点击事件
        val intent = Intent(context, WifiInfoWidget::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.btn_refresh, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun getWifiDetails(context: Context): Triple<String, String, String> {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        val wifiInfo = wifiManager.connectionInfo

        val ssid = if (wifiInfo.ssid != WifiManager.UNKNOWN_SSID) {
            wifiInfo.ssid.trim('"')
        } else {
            "未知 Wi-Fi"
        }

        val ipAddress = if (wifiInfo.ipAddress != 0) {
            val ipBytes =
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(wifiInfo.ipAddress)
                    .array()
            InetAddress.getByAddress(ipBytes).hostAddress ?: "未知"
        } else {
            "未连接"
        }

        val gatewayAddress = if (dhcpInfo.gateway != 0) {
            val gatewayBytes =
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dhcpInfo.gateway)
                    .array()
            InetAddress.getByAddress(gatewayBytes).hostAddress ?: "未知"
        } else {
            "未知"
        }

        return Triple(ssid, ipAddress, gatewayAddress)
    }
}