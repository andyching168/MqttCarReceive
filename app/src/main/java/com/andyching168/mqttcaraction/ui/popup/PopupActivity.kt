package com.andyching168.mqttcaraction.ui.popup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.andyching168.mqttcaraction.ui.theme.MqttCarActionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PopupActivity : ComponentActivity() {

    private val viewModel: PopupViewModel by viewModels()

    companion object {
        const val EXTRA_TEXT_CONTENT = "extra_text_content"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textContent = intent.getStringExtra(EXTRA_TEXT_CONTENT) ?: "No content"

        // 所有 Composable 调用都必须在这个 setContent 块内部
        setContent {
            // 我们需要在这里启动倒计时，以确保它与 Composable 的生命周期绑定
            LaunchedEffect(Unit) {
                viewModel.startOrResetCountdown()
            }

            MqttCarActionTheme {
                // PopupScreen 是 @Composable 函数，所以它必须在这里被调用
                PopupScreen(
                    textContent = textContent,
                    onOkClicked = {
                        viewModel.onOkClicked()
                    },
                    // 将 ViewModel 的方法作为回调传递给 UI
                    onInteraction = {
                        viewModel.startOrResetCountdown()
                    }
                )
            }
        }

        // 观察 ViewModel 的关闭事件
        // 这是处理 Activity 生命周期事件和协程 Flow 的标准、安全做法
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finishEvent.collectLatest {
                    finish() // 收到事件后关闭 Activity
                }
            }
        }
    }
}