package com.macsense.ai

import android.app.Application
import com.macsense.ai.di.AppContainer

class MacSenseApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
