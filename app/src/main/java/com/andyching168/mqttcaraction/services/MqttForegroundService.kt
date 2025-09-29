package com.andyching168.mqttcaraction.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.andyching168.mqttcaraction.R
import com.andyching168.mqttcaraction.TAG
import com.andyching168.mqttcaraction.data.MqttClientWrapper
import com.andyching168.mqttcaraction.data.MqttConnectionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.andyching168.mqttcaraction.data.CarActionMessage
import com.andyching168.mqttcaraction.ui.popup.PopupActivity
import com.andyching168.mqttcaraction.util.isWebUrl
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class MqttForegroundService : Service() {

    @Inject
    lateinit var mqttClientWrapper: MqttClientWrapper

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationManager: NotificationManager? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "MqttServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        Log.i(TAG, "MqttForegroundService created.") // 使用原生 Log
        // 将连接和观察逻辑移出 onCreate
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "MqttForegroundService onStartCommand.") // 使用原生 Log

        // 1. 立即创建通知渠道和通知
        createNotificationChannel()
        val notification = createNotification("服务正在初始化...")

        // 2. 立即将服务提升到前台，这是最优先的操作
        startForeground(NOTIFICATION_ID, notification)
        Log.i(TAG, "startForeground() called.") // 使用原生 Log

        // 3. 在服务已处于前台状态后，再开始执行连接和观察逻辑
        setupMqttClient()

        return START_STICKY
    }

    private fun setupMqttClient() {
        // 在这里启动连接
        serviceScope.launch {
            try {
                mqttClientWrapper.connect()
            } catch (e: Exception) {
                Log.e(TAG, "MQTT connection failed in service", e) // 使用原生 Log
            }
        }
        mqttClientWrapper.connectionState
            .onEach { state ->
                Log.i(TAG, "Connection state changed: $state") // 使用原生 Log
                // 根据连接状态更新通知
                val statusText = when (state) {
                    is MqttConnectionState.Connected -> "已连接"
                    is MqttConnectionState.Connecting -> "连接中..."
                    is MqttConnectionState.Disconnected -> "已断开"
                    is MqttConnectionState.Error -> "连接错误"
                }
                updateNotification(statusText)
            }
            .launchIn(serviceScope)

        mqttClientWrapper.messageFlow
            .onEach { message ->
                Log.d(TAG, "Received parsed message object: $message") // 使用原生 Log
                processMessage(message)
            }
            .launchIn(serviceScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("MqttForegroundService destroyed.")
        serviceScope.cancel()
        mqttClientWrapper.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MQTT Service Channel",
                NotificationManager.IMPORTANCE_LOW // 使用 LOW 可以避免声音提示
            )
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIcon = R.drawable.ic_launcher_foreground

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MqttCarAction")
            .setContentText(contentText)
            .setSmallIcon(notificationIcon)
            .setOngoing(true) // 让通知不能被轻易滑掉
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    private fun processMessage(message: CarActionMessage) {
        // 1. 验证时间戳
        val messageAgeMillis = System.currentTimeMillis() - message.timestamp
        if (messageAgeMillis > TimeUnit.SECONDS.toMillis(60)) {
            Log.w(TAG, "Stale message received, ignoring. Age: ${messageAgeMillis / 1000}s")
            return // 消息太旧，直接返回
        }

        Log.i(TAG, "Fresh message received, processing...")

        // 2. 判断内容类型并执行动作
        if (message.text.isWebUrl()) {
            // 是 URL，打开浏览器
            openUrl(message.text)
        } else {
            // 不是 URL，准备显示弹窗
            showPopup(message.text)
        }
    }
    // --- (新增) 打开 URL 的方法 ---
    private fun openUrl(url: String) {
        Log.i(TAG, "Opening URL: $url")
        // 创建一个用于查看内容的隐式 Intent
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // FLAG_ACTIVITY_NEW_TASK 是必须的，因为我们是从 Service 这个没有
            // UI 上下文的环境中启动 Activity。
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            applicationContext.startActivity(intent) // <--- 修正后的代码
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
            // 可以在这里发送一个 Toast 提示用户没有可以打开此链接的应用
        }
    }
    // --- (新增) 显示弹窗的方法 ---
    private fun showPopup(text: String) {
        Log.i(TAG, "Showing popup with text: $text")

        // 创建一个用于启动 PopupActivity 的显式 Intent
        val intent = Intent(applicationContext, PopupActivity::class.java).apply {
            // 携带要显示的文本内容
            putExtra(PopupActivity.EXTRA_TEXT_CONTENT, text)
            // 必须的 Flag，因为我们从 Service 启动 Activity
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // (可选) 如果你希望每次都显示一个新的弹窗而不是复用旧的
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        try {
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show popup", e)
        }
    }
}

