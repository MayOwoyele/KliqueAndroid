package com.justself.klique


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class UserDetailsViewModel : ViewModel() {
    private val _firstName = MutableStateFlow("")
    private val _lastName = MutableStateFlow("")
    val firstName = _firstName.asStateFlow()
    val lastName = _lastName.asStateFlow()

    fun fetchCustomerDetails(customerId: Int) {
        viewModelScope.launch {
            try {
                Log.d("fetchCustomerDetails", "Starting to fetch details for customer ID: $customerId")

                /*val endpoint = "getCustomerDetails/$customerId"
                val response = NetworkUtils.makeRequest(endpoint, "GET", emptyMap())
                // remember to replace with the JsonObject call with 'response' later */
                val fakeJsonResponse = """
                {
                    "user": {
                        "first_name": "Mayokun",
                        "last_name": "Beckley"
                    }
                }
                """
                Log.d("fetchCustomerDetails", "Fake JSON response: $fakeJsonResponse")

                val responseObject = JSONObject(fakeJsonResponse)
                Log.d("fetchCustomerDetails", "Parsed JSONObject: $responseObject")

                if (responseObject.has("user")) {
                    val user = responseObject.getJSONObject("user")
                    Log.d("fetchCustomerDetails", "User JSONObject: $user")

                    _firstName.value = user.getString("first_name")
                    _lastName.value = user.getString("last_name")

                    Log.d("fetchCustomerDetails", "First name: ${_firstName.value}, Last name: ${_lastName.value}")
                } else {
                    val error = responseObject.optString("error", "Unknown error occurred")
                    Log.e("UserDetailsError", "Error fetching customer details: $error")
                }

            } catch (e: Exception) {
                Log.e("UserDetailsError", "Exception in fetching customer details: ${e.message}")
            }
        }
    }
}
