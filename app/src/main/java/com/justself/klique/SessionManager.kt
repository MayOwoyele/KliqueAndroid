package com.justself.klique

import android.content.Context
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

object SessionManager {
    private val _customerId = MutableStateFlow(1)
    val customerId: StateFlow<Int> = _customerId.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()


    private const val FULL_NAME_KEY = "FULL_NAME_KEY"
    private const val KLIQUE_APP_USER = "KLIQUE_APP_USER"
    private const val CUSTOMER_ID = "CUSTOMER_ID"

    fun setCustomerId(id: Int) {
        _customerId.value = id
    }
    fun setFullName(name: String) {
        _fullName.value = name
    }

    fun fetchCustomerDataFromSharedPreferences() {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        val savedCustomerId = sharedPreferences.getInt(CUSTOMER_ID, -1)
        val savedFullName = sharedPreferences.getString(FULL_NAME_KEY, "") ?: ""

        _customerId.value = savedCustomerId
        _fullName.value = savedFullName
    }

    fun resetCustomerData() {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .remove(CUSTOMER_ID)
            .remove(FULL_NAME_KEY)
            .apply()

        _customerId.value = -1
        _fullName.value = ""
    }
    fun saveCustomerIdToSharedPreferences(customerId: Int) {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(CUSTOMER_ID, customerId)
        editor.apply()
    }
    fun saveNameToSharedPreferences(name: String) {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(FULL_NAME_KEY, name)
        editor.apply()
    }
}
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
    private fun fetchRequiredVersionFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = mapOf("platform" to "android")
                val response =
                    NetworkUtils.makeRequest("fetchRequiredVersion", KliqueHttpMethod.GET, params)
                if (response.first) {
                    val jsonObject = JSONObject(response.second)
                    val requiredVersion = jsonObject.getInt("minimumVersion")
                    val expirationDate = jsonObject.getString("expirationDate").toLong()
                    val sharedPreferences =
                        appContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    sharedPreferences.edit().putInt("REQUIRED_VERSION", requiredVersion).apply()
                    saveExpirationDate(appContext, expirationDate)
                }
            } catch (e: Exception) {
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