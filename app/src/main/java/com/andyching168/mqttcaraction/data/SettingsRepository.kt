package com.andyching168.mqttcaraction.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 通过委托创建 DataStore 实例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mqtt_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 定义 DataStore 中每个键的类型和名称
    private object Keys {
        val MQTT_HOST = stringPreferencesKey("mqtt_host")
        val MQTT_PORT = intPreferencesKey("mqtt_port")
        val MQTT_TOPIC = stringPreferencesKey("mqtt_topic")
        val MQTT_USERNAME = stringPreferencesKey("mqtt_username")
        val MQTT_PASSWORD = stringPreferencesKey("mqtt_password")
    }

    // 将 DataStore 中的数据流转换为我们自己的 MqttSettings 数据流
    val mqttSettingsFlow: Flow<MqttSettings> = context.dataStore.data
        .map { preferences ->
            MqttSettings(
                host = preferences[Keys.MQTT_HOST] ?: "test.mosquitto.org",
                port = preferences[Keys.MQTT_PORT] ?: 1883,
                topic = preferences[Keys.MQTT_TOPIC] ?: "carTasker/message",
                username = preferences[Keys.MQTT_USERNAME] ?: "",
                password = preferences[Keys.MQTT_PASSWORD] ?: ""
            )
        }

    // 提供一个 suspend 函数来更新设置
    suspend fun updateSettings(settings: MqttSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MQTT_HOST] = settings.host
            preferences[Keys.MQTT_PORT] = settings.port
            preferences[Keys.MQTT_TOPIC] = settings.topic
            preferences[Keys.MQTT_USERNAME] = settings.username
            preferences[Keys.MQTT_PASSWORD] = settings.password
        }
    }
}