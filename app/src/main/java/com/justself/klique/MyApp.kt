package com.justself.klique

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp

class MyKliqueApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        FirebaseApp.initializeApp(this)
        NetworkUtils.initialize(this)
        AppUpdateManager.initialize()
        SessionManager.startSession()
    }
    companion object {
        lateinit var appContext: Context
            private set
    }
}