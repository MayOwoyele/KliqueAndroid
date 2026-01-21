package com.justself.klique

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.FirebaseApp
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.SessionManager.customerId
import com.justself.klique.SessionManager.fullName

class MyKliqueApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        FirebaseApp.initializeApp(this)
        NetworkUtils.initialize(this)
        AppUpdateManager.initialize()
        CliqueScreenObject.initialize()
        DroidAppUpdateManager.initCore(this)
        SessionManager.startSession()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val crashLog = """
                🚨 UNHANDLED CRASH 🚨
                🧵 Thread: ${thread.name}
                ❌ Exception: ${throwable.localizedMessage}
                🔍 Stacktrace:
                ${Log.getStackTraceString(throwable)}
            """.trimIndent()
            Log.e("CrashLogger", crashLog)
            sendCrashReportByEmail(crashLog)
        }
        scheduleDiaryBackupWork(appContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleTracker)
    }

    private fun sendCrashReportByEmail(crashLog: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@kliquesocial.com")) // Replace with your email
            putExtra(Intent.EXTRA_SUBJECT, "🔥 Klique App Crash Report")
            putExtra(Intent.EXTRA_TEXT, crashLog)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Crash Report"))
        } catch (e: Exception) {
            Log.e("CrashLogger", "No email client found", e)
        }
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
object AppLifecycleTracker : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        if (!WebSocketManager.isConnected.value) {
            WebSocketManager.connect(customerId.value, fullName.value, appContext, "Lifecycle")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Logger.d("AppLifecycleTracker", "App entered background")
        WebSocketManager.close()
    }
}