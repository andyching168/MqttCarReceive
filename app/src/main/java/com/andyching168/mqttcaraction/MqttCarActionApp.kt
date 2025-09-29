package com.andyching168.mqttcaraction

import android.app.Application
import android.util.Log
import com.arcao.slf4j.timber.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MqttCarActionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            // 为了验证 Application 类本身被执行，我们也加一条原生 Log
            Log.d("MQTT_APP_TEST", "Application onCreate: Timber planted.")
        }
    }
}