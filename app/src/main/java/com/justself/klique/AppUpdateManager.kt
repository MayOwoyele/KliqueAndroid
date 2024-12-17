package com.justself.klique

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

object AppUpdateManager {
    private const val EXPIRATION_DATE_KEY = "EXPIRATION_DATE"
    private const val SHARED_PREFS_NAME = "APP_PREFS"
    val updateDismissedFlow = MutableStateFlow(false)

    private fun saveExpirationDate(context: Context, expirationDateMillis: Long) {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putLong(EXPIRATION_DATE_KEY, expirationDateMillis)
            .apply()
    }
    fun initialize(){
        val randomNumber = Random.nextInt(1, 101)
        if (randomNumber < 25) {
            fetchRequiredVersionFromServer()
        }
    }
    fun getExpirationDate(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(EXPIRATION_DATE_KEY, 0L)
    }
    fun fetchRequiredVersionFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = mapOf("platform" to "android")
                val response =
                    NetworkUtils.makeRequest("fetchRequiredVersion", KliqueHttpMethod.GET, params)
                if (response.first) {
                    Log.d("Minumum version", response.second)
                    val jsonObject = JSONObject(response.second)
                    val requiredVersion = jsonObject.getInt("minimumVersion")
                    val expirationDate = jsonObject.getString("expirationDate").toLong()
                    val sharedPreferences =
                        MyKliqueApp.appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    sharedPreferences.edit().putInt("REQUIRED_VERSION", requiredVersion).apply()
                    saveExpirationDate(MyKliqueApp.appContext, expirationDate)
                }
            } catch (e: Exception) {
                Log.d("Minumum version", e.toString())
                e.printStackTrace()
            }
        }
    }
    fun getRequiredVersion(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt("REQUIRED_VERSION", 1)
    }
    fun isUpdateRequired(context: Context): Boolean {
        val currentVersion = BuildConfig.VERSION_CODE
        val requiredVersion = getRequiredVersion(context)
        val expirationDate = getExpirationDate(context)
        val twoWeeksMillis = 14 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        return currentVersion < requiredVersion && (expirationDate - currentTime <= twoWeeksMillis)
    }
    fun dismissUpdateRequirement() {
        updateDismissedFlow.value = true
    }
}