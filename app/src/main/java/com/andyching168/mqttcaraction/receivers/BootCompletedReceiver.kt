package com.andyching168.mqttcaraction.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.andyching168.mqttcaraction.TAG
import com.andyching168.mqttcaraction.services.MqttForegroundService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ！！注意：Hilt 目前无法直接在 BroadcastReceiver 中注入依赖。
// 我们将使用一个入口点来手动获取依赖。
// 这是一个标准的 Hilt 解决方法。

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        // 1. 确认收到的广播是我们想要的“开机完成”广播
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed event received.")

            // 2. 创建一个 Intent 来启动我们的 Service
            val serviceIntent = Intent(context, MqttForegroundService::class.java)

            // 3. 启动 Service
            // 从 Android 8.0 (Oreo) 开始，后台启动 Service 有限制，
            // 必须使用 startForegroundService。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Log.i(TAG, "Starting foreground service on boot...")
            } else {
                // 对于旧版本系统，直接启动
                context.startService(serviceIntent)
                Log.i(TAG, "Starting service on boot...")
            }
        }
    }
}