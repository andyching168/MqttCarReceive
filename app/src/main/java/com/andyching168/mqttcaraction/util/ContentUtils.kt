package com.andyching168.mqttcaraction.util

import android.util.Patterns

/**
 * 检查给定的字符串是否为一个有效的 Web URL。
 * @return 如果是 URL 则返回 true，否则返回 false。
 */
fun String.isWebUrl(): Boolean {
    // Patterns.WEB_URL.matcher(this).matches() 是 Android SDK 提供的一个
    // 非常方便且可靠的 URL 正则表达式匹配工具。
    return Patterns.WEB_URL.matcher(this).matches()
}