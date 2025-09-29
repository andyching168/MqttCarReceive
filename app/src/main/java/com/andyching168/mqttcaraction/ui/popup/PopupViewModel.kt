package com.andyching168.mqttcaraction.ui.popup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PopupViewModel @Inject constructor() : ViewModel() {

    private val _finishEvent = MutableSharedFlow<Unit>()
    val finishEvent = _finishEvent.asSharedFlow()

    private var countdownJob: Job? = null

    // **修正点 1:** 将常量改为普通的私有属性
    private val countdownDurationMs = 15_000L

    fun startOrResetCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            delay(countdownDurationMs)
            _finishEvent.emit(Unit)
        }
    }

    fun onOkClicked() {
        // **修正点 2:** 确保点击 OK 时也取消计时器并发送关闭事件
        countdownJob?.cancel()
        viewModelScope.launch {
            _finishEvent.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}