package com.andyching168.mqttcaraction.data

import android.util.Log
import com.andyching168.mqttcaraction.TAG
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val hivemq: Any
    get() {
        TODO()
    }

@Singleton
class MqttClientWrapper @Inject constructor(private val settingsRepository: SettingsRepository) { // 不再需要 Context

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected(null))
    val connectionState = _connectionState.asStateFlow()

    private val _messageFlow = MutableSharedFlow<CarActionMessage>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private var client: Mqtt5AsyncClient? = null
    private var currentSettings: MqttSettings? = null
    init {
        scope.launch {
            settingsRepository.mqttSettingsFlow.collectLatest { settings ->
                Log.d(TAG, "New settings collected: $settings")
                val oldSettings = currentSettings
                currentSettings = settings
                // 只有当 host, port, 或认证信息这些关键连接参数变化时才重连
                if (settings.host != oldSettings?.host ||
                    settings.port != oldSettings.port ||
                    settings.username != oldSettings.username ||
                    settings.password != oldSettings.password
                ) {
                    disconnect()
                    connect()
                } else if (settings.topic != oldSettings.topic) {
                    // 如果只是 topic 变了，则只需要重新订阅
                    subscribeToTopic()
                }
            }
        }
    }



    suspend fun connect() {
        val settings = currentSettings ?: settingsRepository.mqttSettingsFlow.first()
        currentSettings = settings

        if (client?.state?.isConnectedOrReconnect == true) {
            Log.i(TAG, "Client is already connected or reconnecting.")
            return
        }

        Log.i(TAG, "MQTT client building and connecting to ${settings.host}:${settings.port}...")

        client = MqttClient.builder()
            .useMqttVersion5()
            .identifier(UUID.randomUUID().toString())
            .serverHost(settings.host)
            .serverPort(settings.port)
            .addConnectedListener {
                Log.i(TAG, "HiveMQ client connected!")
                scope.launch {
                    _connectionState.emit(MqttConnectionState.Connected)
                    subscribeToTopic()
                }
            }
            .addDisconnectedListener { context ->
                val cause = context.cause
                Log.w(TAG, "HiveMQ client disconnected!", cause)
                scope.launch {
                    _connectionState.emit(MqttConnectionState.Disconnected(cause))
                }
            }
            .buildAsync()

        // **↓↓↓ 核心修正点在这里 ↓↓↓**

        // 1. 创建一个可修改的连接请求构建器
        val connectBuilder = client!!.connectWith()

        // 2. 如果用户名不为空，才配置认证信息
        if (settings.username.isNotBlank()) {
            Log.i(TAG, "Connecting with username: ${settings.username}")
            connectBuilder
                .simpleAuth()
                .username(settings.username) // 在这里 username 不可能是 null
                .password(settings.password.toByteArray()) // password 也不可能是 null
                .applySimpleAuth()
        } else {
            Log.i(TAG, "Connecting anonymously.")
        }

        // 3. 发送最终构建好的连接请求
        val connAck: Mqtt5ConnAck = connectBuilder.send().await()
        Log.i(TAG, "MQTT connect acknowledged: $connAck")

        if (connAck.reasonCode.isError) {
            val error = RuntimeException("MQTT connection failed: ${connAck.reasonCode} - ${connAck.reasonString}")
            Log.e(TAG, "Connection failure", error)
            _connectionState.emit(MqttConnectionState.Error(error))
            throw error
        }
    }

    private suspend fun subscribeToTopic() {
        val topic = currentSettings?.topic ?: return
        client?.let {
            Log.i(TAG, "Subscribing to topic: $topic")
            it.subscribeWith()
                .topicFilter(topic)
                .callback { publish -> handleIncomingMessage(publish) }
                .send()
                .await()
            Log.i(TAG, "Successfully subscribed to topic: $topic")
        }
    }

    private fun handleIncomingMessage(publish: Mqtt5Publish) {
        val payload = StandardCharsets.UTF_8.decode(publish.payload.get()).toString()
        Log.d(TAG, "HiveMQ raw message arrived. Topic: ${publish.topic}, Message: $payload") // 使用原生 Log
        scope.launch {
            try {
                val parsedMessage = jsonParser.decodeFromString<CarActionMessage>(payload)
                _messageFlow.emit(parsedMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse HiveMQ message: $payload", e) // 使用原生 Log
            }
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null // 确保客户端对象被清空
    }
}