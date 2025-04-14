package com.justself.klique

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject


object JWTNetworkCaller {
    private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"
    private const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN_KEY"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("JWT_PREFS", Context.MODE_PRIVATE)
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken).apply()
    }

    private fun fetchRefreshToken(context: Context): String? {
        val prefs = getPreferences(context)
        return prefs.getString(REFRESH_TOKEN_KEY, null)
    }

    fun fetchAccessToken(): String? {
        val prefs = getPreferences(appContext)
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    /** action should be the action you want to take if response 200
        response itself should be your network attempt, since it returns a response of type Triple
        errorAction should be the action you want to take when there's not a token problem, but
        still an error
        just put each of these actions in the lambda body, calling performReusableNetworkCalls with them
         **/
    suspend fun performReusableNetworkCalls(
        response: suspend () -> NetworkUtils.JwtTriple,
        action: suspend (NetworkUtils.JwtTriple) -> Unit,
        errorAction: suspend (NetworkUtils.JwtTriple) -> Unit
    ) {
        val theResponse = response()
        if (theResponse is NetworkUtils.JwtTriple.Value) {
            loggerD("refreshToken"
            ) { "response: ${theResponse.response}, code: ${theResponse.responseCode}" }

            when (theResponse.responseCode) {
                200 -> action(theResponse)
                401 -> tokenIssue(action, response, errorAction)
                403 -> SessionManager.resetCustomerData()
                else -> errorAction(theResponse)
            }
        } else {
            Log.e("refreshToken", "Unknown JwtTriple subclass: $theResponse")
            errorAction(theResponse)
        }
    }

    private suspend fun tokenIssue(
        action: suspend (NetworkUtils.JwtTriple) -> Unit,
        response: suspend () -> NetworkUtils.JwtTriple,
        errorAction: suspend (NetworkUtils.JwtTriple) -> Unit
    ) {
        val statusCode = refreshAccessToken()
        Log.d("refreshToken", "status code : $statusCode")

        if (statusCode == 403) {
            SessionManager.resetCustomerData()
            return
        }

        val responseTriple = response()
        if (responseTriple !is NetworkUtils.JwtTriple.Value) {
            Log.e("tokenIssue", "Unexpected JwtTriple type: $responseTriple")
            errorAction(responseTriple)
            return
        }

        when (responseTriple.responseCode) {
            200 -> action(responseTriple)
            401 -> {
                val retryResponse = response()
                if (retryResponse !is NetworkUtils.JwtTriple.Value) {
                    Log.e("tokenIssue", "Unexpected JwtTriple type on retry: $retryResponse")
                    errorAction(retryResponse)
                    return
                }

                if (retryResponse.isSuccessful) {
                    action(retryResponse)
                } else {
                    errorAction(responseTriple)
                }
            }

            403 -> SessionManager.resetCustomerData()
            else -> errorAction(responseTriple)
        }
    }

    private val tokenMutex = Mutex()
    private var isRefreshingToken = false
    suspend fun refreshAccessToken(): Int {
        var shouldWait = false
        tokenMutex.withLock {
            if (isRefreshingToken) {
                shouldWait = true
            } else {
                isRefreshingToken = true
            }
        }
        if (shouldWait) {
            delay(1000)
            return 200
        }
        try {
            val refreshToken = fetchRefreshToken(appContext)
            if (refreshToken != null) {
                Log.d("refreshToken", "the actual token: $refreshToken")
                try {
                    val response = NetworkUtils.makeRequest(
                        "refreshToken",
                        KliqueHttpMethod.POST,
                        emptyMap(),
                        jsonBody = JSONObject().put("refreshToken", refreshToken)
                            .put("userId", SessionManager.customerId.value).toString()
                    )
                    Log.d("refreshToken", response.second)
                    if (response.third == 200) {
                        val newAccessToken = JSONObject(response.second).getString("accessToken")
                        val newRefreshToken = JSONObject(response.second).getString("refreshToken")
                        Log.d("refreshToken", "new refresh token $newRefreshToken")
                        saveTokens(appContext, newAccessToken, newRefreshToken)
                        Log.d("refreshToken", "Check saved token ${fetchRefreshToken(appContext)}")
                    }
                    return response.third
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return 400
        } finally {
            tokenMutex.withLock {
                isRefreshingToken = false
            }
        }
    }
}