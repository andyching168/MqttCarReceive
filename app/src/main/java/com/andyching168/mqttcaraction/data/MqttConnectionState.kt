package com.andyching168.mqttcaraction.data

sealed class MqttConnectionState {
    // 等待连接或正在连接
    data object Connecting : MqttConnectionState()
    // 成功连接
    data object Connected : MqttConnectionState()
    // 连接断开
    data class Disconnected(val cause: Throwable?) : MqttConnectionState()
    // 发生错误
    data class Error(val exception: Throwable) : MqttConnectionState()
}