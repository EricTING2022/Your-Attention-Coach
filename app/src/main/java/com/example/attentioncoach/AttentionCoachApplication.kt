package com.example.attentioncoach

import android.app.Application

class AttentionCoachApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
