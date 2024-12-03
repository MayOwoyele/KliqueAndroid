package com.justself.klique

import android.content.Context
import android.telephony.TelephonyManager
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

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
    fun fetchCustomerDataFromSharedPreferences() {
        val sharedPreferences = appContext.getSharedPreferences(KLIQUE_APP_USER, Context.MODE_PRIVATE)
        val savedCustomerId = sharedPreferences.getInt(CUSTOMER_ID, -1)
        val savedFullName = sharedPreferences.getString(FULL_NAME_KEY, "") ?: ""

        _customerId.value =  1//savedCustomerId
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
    fun getUserCountryCode(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkCountry = telephonyManager.networkCountryIso?.uppercase()
        val savedCountry = getCountryFromSharedPreferences()
        return networkCountry ?: savedCountry
    }
}