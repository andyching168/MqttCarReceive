package com.andyching168.mqttcaraction.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andyching168.mqttcaraction.data.MqttSettings
import com.andyching168.mqttcaraction.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // 将 Repository 中的 Flow 转换为 StateFlow，以便 Compose UI 可以轻松地观察它
    val settingsState = settingsRepository.mqttSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 5秒后如果没有观察者则停止
            initialValue = MqttSettings() // 提供一个初始默认值
        )

    // UI 调用这个方法来请求更新设置
    fun onSettingsChanged(newSettings: MqttSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
        }
    }
}