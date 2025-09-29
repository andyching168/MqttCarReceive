package com.andyching168.mqttcaraction.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CarActionMessage(
    val text: String,
    val timestamp: Long
)