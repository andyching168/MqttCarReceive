package com.andyching168.mqttcaraction.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.andyching168.mqttcaraction.ui.theme.MqttCarActionTheme
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent // 需要 import
import com.andyching168.mqttcaraction.services.MqttForegroundService // 需要 import

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MqttCarActionTheme {
                // 从 ViewModel 中收集状态
                val settings by viewModel.settingsState.collectAsState()

                SettingsScreen(
                    currentSettings = settings,
                    onSave = { newSettings ->
                        viewModel.onSettingsChanged(newSettings)
                        // 保存后，重启服务以应用新设置
                        restartMqttService()
                        // (可选) 关闭设置页面
                        finish()
                    }
                )
            }
        }
    }

    private fun restartMqttService() {
        // 先停止
        stopService(Intent(this, MqttForegroundService::class.java))
        // 再启动
        startForegroundService(Intent(this, MqttForegroundService::class.java))
    }
}