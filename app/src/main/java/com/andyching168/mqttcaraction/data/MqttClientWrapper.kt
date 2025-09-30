package com.andyching168.mqttcaraction.data

import android.util.Log
import com.andyching168.mqttcaraction.TAG
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttClientWrapper @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clientMutex = Mutex()

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected(null))
    val connectionState = _connectionState.asStateFlow()

    private val _messageFlow = MutableSharedFlow<CarActionMessage>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private var client: Mqtt5AsyncClient? = null
    private var currentSettings: MqttSettings? = null
    private val isManuallyDisconnected = AtomicBoolean(false)

    init {
        settingsRepository.mqttSettingsFlow
            .onEach { settings ->
                Log.d(TAG, "New settings collected: $settings")
                val oldSettings = currentSettings
                currentSettings = settings
                if (settings.host != oldSettings?.host ||
                    settings.port != oldSettings.port ||
                    settings.username != oldSettings.username ||
                    settings.password != oldSettings.password
                ) {
                    reconnect()
                } else if (settings.topic != oldSettings?.topic && client?.state?.isConnected == true) {
                    scope.launch { subscribeToTopic() }
                }
            }
            .launchIn(scope)

        startConnectionSupervisor()
    }

    private fun startConnectionSupervisor() {
        scope.launch {
            while (isActive) {
                delay(30_000)
                if (!isManuallyDisconnected.get() && client?.state?.isConnectedOrReconnect != true) {
                    Log.w(TAG, "Connection supervisor: Detected disconnected state. Forcing reconnect...")
                    connect()
                }
            }
        }
    }

    suspend fun connect() {
        clientMutex.withLock {
            if (isManuallyDisconnected.get()) {
                Log.i(TAG, "Connection attempt ignored due to manual disconnection.")
                return@withLock
            }
            if (client?.state?.isConnectedOrReconnect == true) {
                Log.i(TAG, "Client is already connected or reconnecting.")
                return@withLock
            }

            val settings = currentSettings ?: settingsRepository.mqttSettingsFlow.first()
            currentSettings = settings

            try {
                Log.i(TAG, "MQTT client building and connecting to ${settings.host}:${settings.port}...")

                val localClient = MqttClient.builder()
                    .useMqttVersion5()
                    .identifier(UUID.randomUUID().toString())
                    .serverHost(settings.host)
                    .serverPort(settings.port)
                    .addConnectedListener { onConnect() }
                    .addDisconnectedListener { onDisconnect(it) }
                    .buildAsync()

                client = localClient

                val connectBuilder = localClient.connectWith()

                if (settings.username.isNotBlank()) {
                    Log.i(TAG, "Connecting with username: ${settings.username}")
                    connectBuilder
                        .simpleAuth()
                        .username(settings.username)
                        .password(settings.password.toByteArray())
                        .applySimpleAuth()
                } else {
                    Log.i(TAG, "Connecting anonymously.")
                }

                val connAck: Mqtt5ConnAck = connectBuilder.send().await()
                Log.i(TAG, "MQTT connect acknowledged: $connAck")

                if (connAck.reasonCode.isError) {
                    throw RuntimeException("MQTT connection failed: ${connAck.reasonCode} - ${connAck.reasonString}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection attempt failed.", e)
                _connectionState.emit(MqttConnectionState.Error(e))
            }
        }
    }

    fun reconnect() {
        scope.launch {
            Log.i(TAG, "Reconnect requested.")
            isManuallyDisconnected.set(false)
            disconnectInternal()
            connect()
        }
    }

    fun disconnect() {
        scope.launch {
            Log.i(TAG, "Manual disconnect requested.")
            isManuallyDisconnected.set(true)
            disconnectInternal()
        }
    }

    private suspend fun disconnectInternal() {
        clientMutex.withLock {
            val localClient = client
            if (localClient != null) {
                Log.i(TAG, "Disconnecting internal client...")
                try {
                    localClient.disconnect().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error during disconnect", e)
                }
            }
            client = null
            _connectionState.emit(MqttConnectionState.Disconnected(null))
        }
    }

    private fun onConnect() {
        Log.i(TAG, "HiveMQ client connected!")
        scope.launch {
            _connectionState.emit(MqttConnectionState.Connected)
            subscribeToTopic()
        }
    }

    private fun onDisconnect(context: com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext) {
        val cause = context.cause
        Log.w(TAG, "HiveMQ client disconnected!", cause)
        scope.launch {
            _connectionState.emit(MqttConnectionState.Disconnected(cause))
        }
    }

    private suspend fun subscribeToTopic() {
        clientMutex.withLock {
            val topic = currentSettings?.topic ?: return@withLock
            val localClient = client ?: return@withLock

            if (localClient.state.isConnected) {
                try {
                    Log.i(TAG, "Subscribing to topic: $topic")
                    localClient.subscribeWith()
                        .topicFilter(topic)
                        .callback { publish -> handleIncomingMessage(publish) }
                        .send()
                        .await()
                    Log.i(TAG, "Successfully subscribed to topic: $topic")
                } catch (e: Exception) {
                    Log.e(TAG, "Subscription failed", e)
                }
            } else {
                Log.w(TAG, "Subscription skipped: client is not connected.")
            }
        }
    }

    private fun handleIncomingMessage(publish: Mqtt5Publish) {
        val payload = StandardCharsets.UTF_8.decode(publish.payload.get()).toString()
        Log.d(TAG, "HiveMQ raw message arrived. Topic: ${publish.topic}, Message: $payload")
        scope.launch {
            try {
                val parsedMessage = jsonParser.decodeFromString<CarActionMessage>(payload)
                _messageFlow.emit(parsedMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse HiveMQ message: $payload", e)
            }
        }
    }
}