package com.justself.klique

import android.content.Context
import android.content.SharedPreferences
import com.justself.klique.MyKliqueApp.Companion.appContext
import org.json.JSONObject


object JWTNetworkCaller{
    private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"
    private const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN_KEY"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("JWT_PREFS", Context.MODE_PRIVATE)
    }
    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .apply()
    }
    private fun fetchRefreshToken(context: Context): String? {
        val prefs = getPreferences(context)
        return prefs.getString(REFRESH_TOKEN_KEY, null)
    }

    fun fetchAccessToken(): String? {
        val prefs = getPreferences(appContext)
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }
    suspend fun performReusableNetworkCalls(
        response: suspend () -> Triple<Boolean, String, Int>,
        action: suspend (Triple<Boolean, String, Int>) -> Unit,
        errorAction: suspend (Triple<Boolean, String, Int>) -> Unit
    ){
        val theResponse = response()
        when (theResponse.third) {
            200 -> action(theResponse)
            401 -> tokenIssue(action, response, errorAction)
            413 -> SessionManager.resetCustomerData()
            else -> errorAction(theResponse)
        }
    }
    /* action should be the action you want to take if response 200
        response itself should be your network attempt, since it returns a response of type Triple
        errorAction should be the action you want to take when there's not a token problem, but
        still an error
        just put each of these actions in the lambda body, calling performReusableNetworkCalls with them
         */
    //the part of reissuing the token up here
    private suspend fun tokenIssue(
        action: suspend (Triple<Boolean, String, Int>) -> Unit,
        response: suspend () -> Triple<Boolean, String, Int>,
        errorAction: suspend (Triple<Boolean, String, Int>) -> Unit
    ) {
        if (refreshAccessToken()) {
            val responseTriple = response()
            when (responseTriple.third) {
                200 -> action(responseTriple)
                401 -> {
                    val retryResponse = response()
                    if (retryResponse.first) {
                        action(retryResponse)
                    } else {
                        errorAction(responseTriple)
                    }
                }
                else -> errorAction(responseTriple)
            }
        }
    }
    private suspend fun refreshAccessToken(): Boolean {
        val refreshToken = fetchRefreshToken(appContext)
        if (refreshToken != null) {
            val response = NetworkUtils.makeRequest(
                "refreshTokenEndpoint",
                KliqueHttpMethod.POST,
                emptyMap(),
                jsonBody = """{ "refreshToken": "$refreshToken" }"""
            )
            if (response.first) {
                val newAccessToken = JSONObject(response.second).getString("accessToken")
                val newRefreshToken = JSONObject(response.second).getString("refreshToken")
                saveTokens(appContext, newAccessToken, newRefreshToken)
                return true
            }
        }
        return false
    }
}