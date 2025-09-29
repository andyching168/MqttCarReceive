package com.andyching168.mqttcaraction.data

data class MqttSettings(
    val host: String = "test.mosquitto.org",
    val port: Int = 1883,
    val topic: String = "carTasker/message",
    val username: String = "",
    val password: String = ""
)