package com.andyching168.mqttcaraction.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andyching168.mqttcaraction.data.MqttSettings
import com.andyching168.mqttcaraction.ui.theme.MqttCarActionTheme
import androidx.compose.ui.text.input.PasswordVisualTransformation // <-- 需要 import
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: MqttSettings,
    onSave: (MqttSettings) -> Unit
) {
    // 使用 remember 来管理每个输入框的本地状态
    var host by remember(currentSettings.host) { mutableStateOf(currentSettings.host) }
    var port by remember(currentSettings.port) { mutableStateOf(currentSettings.port.toString()) }
    var topic by remember(currentSettings.topic) { mutableStateOf(currentSettings.topic) }
    var username by remember(currentSettings.username) { mutableStateOf(currentSettings.username) }
    var password by remember(currentSettings.password) { mutableStateOf(currentSettings.password) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("MQTT Host") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic") },
                modifier = Modifier.fillMaxWidth()
            )

            // ↓↓↓ 新增 Username 输入框 ↓↓↓
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // ↓↓↓ 新增 Password 输入框 ↓↓↓
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (Optional)") },
                // 使用密码视觉效果，隐藏输入内容
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val newSettings = MqttSettings(
                        host = host,
                        port = port.toIntOrNull() ?: 1883,
                        topic = topic,
                        // ↓↓↓ 保存新字段的值 ↓↓↓
                        username = username,
                        password = password
                    )
                    onSave(newSettings)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save and Restart Service")
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    MqttCarActionTheme {
        SettingsScreen(currentSettings = MqttSettings(), onSave = {})
    }
}