package com.justself.klique

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.core.content.edit

object SessionManager {
    private val _customerId = MutableStateFlow(0)
    val customerId: StateFlow<Int> = _customerId.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private const val FULL_NAME_KEY = "FULL_NAME_KEY"
    private const val KLIQUE_APP_USER = "KLIQUE_APP_USER"
    private const val CUSTOMER_ID = "CUSTOMER_ID"
    private const val COUNTRY_KEY = "COUNTRY_KEY"

    fun startSession(){
        fetchCustomerDataFromSharedPreferences()
    }
    private const val KEY_CONTACTS_OFFLOADED = "contacts_offloaded"
    private val prefs = appContext.getSharedPreferences("klique", Context.MODE_PRIVATE)
    val contactsOffloadedFlow = MutableStateFlow(prefs.getBoolean(KEY_CONTACTS_OFFLOADED, false))

    fun markContactsOffloaded() {
        prefs.edit { putBoolean(KEY_CONTACTS_OFFLOADED, true) }
        contactsOffloadedFlow.value = true
    }
    fun fetchCustomerDataFromSharedPreferences() {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        val savedCustomerId = sharedPreferences.getInt(CUSTOMER_ID, -1)
        val savedFullName = sharedPreferences.getString(FULL_NAME_KEY, "") ?: ""

        _customerId.value = 1//savedCustomerId
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
        WebSocketManager.close()
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
    fun saveCountryToSharedPreferences(country: String) {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(COUNTRY_KEY, country)
            .apply()
    }
    private fun getCountryFromSharedPreferences(): String {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        return sharedPreferences.getString(COUNTRY_KEY, "") ?: ""
    }
    fun getUserCountryCode(): String {
        val telephonyManager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkCountry = telephonyManager.networkCountryIso?.uppercase()
        val savedPreferences = getCountryFromSharedPreferences()
        val savedCountry = savedPreferences.ifEmpty {
            "NG"
        }
        return networkCountry ?: savedCountry
    }
    const val GLOBAL_CHAR_LIMIT = 5000
    fun sendDeviceTokenToServer() {
        val sharedPreferences = appContext.getSharedPreferences(KliqueFirebaseMessagingService.FIREBASE_PREFS_KEY, Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(KliqueFirebaseMessagingService.FIREBASE_TOKEN_KEY, null)

        if (!token.isNullOrEmpty()) {
            Logger.d("Token", "Sending token to server: $token")

            CoroutineScope(Dispatchers.IO).launch {
                val userId = customerId.value

                if (userId <= 0) {
                    Log.e("SendToken", "Invalid user ID. Cannot send token to server.")
                    return@launch
                }

                val jsonBody = JSONObject().apply {
                    put("userId", userId)
                    put("token", token)
                }.toString()

                try {
                    NetworkUtils.makeJwtRequest(
                        endpoint = "updateDeviceToken",
                        method = KliqueHttpMethod.POST,
                        params = emptyMap(),
                        jsonBody = jsonBody,
                        action = { response ->
                            Logger.d("SendToken", "Token successfully sent to server: ${response.toNetworkTriple().second}")
                        },
                        errorAction = { errorResponse ->
                            Log.e("SendToken", "Error sending token: ${errorResponse.toNetworkTriple().second}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e("SendToken", "Failed to send token to server: ${e.message}")
                }
            }
        } else {
            Logger.d("Token", "No token found to send to server.")
        }
    }
}