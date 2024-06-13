package com.justself.klique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import android.util.Log  // Import Android Log utility

class AuthViewModel : ViewModel() {
    private val _loginState = MutableStateFlow(LoginState.IDLE)
    val loginState = _loginState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _customerId = MutableStateFlow<Int>(25)
    val customerId = _customerId.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.LOADING

            try {
                val params = mapOf("action" to "login", "email" to email, "password" to password)
                val response = NetworkUtils.makeRequest("api.php", "POST", params)
                val responseObject = JSONObject(response)

                if (responseObject.has("user")) {
                    val user = responseObject.getJSONObject("user")
                    if (user.has("customer_id")) {  // Correctly use the key from the server
                        val customerIdValue = user.getInt("customer_id")  // Extract customer ID using correct key
                        _customerId.value = customerIdValue
                        _loginState.value = LoginState.SUCCESS
                        _isLoggedIn.value = true
                        Log.d("LoginSuccess", "Logged in with customer ID: $customerIdValue")  // Log customer ID on success
                    } else {
                        _loginState.value = LoginState.ERROR
                        _isLoggedIn.value = false
                        Log.e("LoginError", "User object did not include customer_id")  // Log an error if customer_id is missing
                    }
                } else {
                    val error = responseObject.optString("error", "Unknown error occurred")
                    _errorMessage.value = error
                    _loginState.value = LoginState.ERROR
                    _isLoggedIn.value = false
                    Log.e("LoginError", "Login failed: $error")  // Log the error message
                }
            } catch (e: IOException) {
                _errorMessage.value = e.message ?: "Network error or invalid response"
                _loginState.value = LoginState.ERROR
                _isLoggedIn.value = false
                Log.e("LoginError", "IOException during login: ${e.message}")  // Log IOException details
            } catch (e: Exception) {
                _errorMessage.value = "An unexpected error occurred"
                _loginState.value = LoginState.ERROR
                _isLoggedIn.value = false
                Log.e("LoginError", "Unexpected error during login: ${e.message}")  // Log unexpected Exception details
            }
        }
    }
}

enum class LoginState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}
