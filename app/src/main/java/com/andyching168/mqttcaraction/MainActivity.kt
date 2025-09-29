package com.andyching168.mqttcaraction

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.andyching168.mqttcaraction.services.MqttForegroundService
import com.andyching168.mqttcaraction.ui.theme.MqttCarActionTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import android.util.Log // <-- 确保导入 Android 的 Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import com.andyching168.mqttcaraction.ui.settings.SettingsActivity // 确保 import


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MqttCarActionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 我们恢复使用 Box 居中，这更稳定
                    ServiceController()
                }
            }
        }
    }
}

@Composable
fun ServiceController() {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 启动服务按钮
            Button(onClick = {
                Log.d(TAG, "Start Service button clicked.")
                val serviceIntent = Intent(context, MqttForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }) {
                Text("Start MQTT Service")
            }

            // **新增:** 跳转到设置页面的按钮
            Button(onClick = {
                Log.d(TAG, "Settings button clicked.")
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }) {
                Text("Settings")
            }
        }
    }
}