package com.andyching168.mqttcaraction.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// ↓↓↓ 确保有这个 import ↓↓↓
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andyching168.mqttcaraction.ui.theme.MqttCarActionTheme

@Composable
fun PopupScreen(
    textContent: String,
    onOkClicked: () -> Unit,
    onInteraction: () -> Unit
) {
    Dialog(onDismissRequest = { /* Do nothing on dismiss */ }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                // ↓↓↓ 核心代码块在这里 ↓↓↓
                .pointerInput(Unit) {
                    // 在这个 lambda 表达式内部, 'this' 的类型是 PointerInputScope
                    // 因此可以调用 awaitPointerEventScope
                    awaitPointerEventScope {
                        while (true) {
                            // 在 awaitPointerEventScope 的内部，'this' 的类型是 AwaitPointerEventScope
                            // 因此可以调用 awaitFirstDown
                            awaitFirstDown(requireUnconsumed = false)
                            onInteraction()
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SelectionContainer {
                    Text(
                        text = textContent,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onOkClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Preview
@Composable
fun PopupScreenPreview() {
    MqttCarActionTheme {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f))) {
            PopupScreen(
                textContent = "这是一个很长很长的测试文本，用于测试滚动功能。这是一个很长很长的测试文本，用于测试滚动功能。这是一个很长很长的测试文本，用于测试滚动功能。",
                onOkClicked = {},
                onInteraction = {}
            )
        }
    }
}