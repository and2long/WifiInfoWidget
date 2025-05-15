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
        val views = RemoteViews(context.packageName, R.layout.wifi_info_widget)
        
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // 获取Wifi信息
        val wifiInfo = wifiManager.connectionInfo
        val networkCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } else null

        // 更新Wifi名称
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null && wifiInfo.ssid != null) {
                wifiInfo.ssid.removeSurrounding("\"")
            } else {
                "未连接"
            }
        } else {
            wifiInfo.ssid?.removeSurrounding("\"") ?: "未连接"
        }
        views.setTextViewText(R.id.wifi_name, "Wifi名称: $ssid")

        // 更新IP地址
        val ipAddress = wifiInfo.ipAddress
        val ipString = if (ipAddress != 0) {
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } else "未获取到"
        views.setTextViewText(R.id.wifi_ip, "IP地址: $ipString")

        // 添加点击刷新功能
        val refreshIntent = Intent(context, WifiInfoWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.wifi_name, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 注册 WiFi 状态变化的广播接收器
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(wifiStateReceiver, filter)

        // 设置定时更新
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WifiInfoWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 每3秒更新一次
        alarmManager.setRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis(),
            3000,
            pendingIntent
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 取消注册广播接收器
        try {
            context.unregisterReceiver(wifiStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 取消定时更新
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WifiInfoWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
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
}