package com.justself.klique


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class UserDetailsViewModel : ViewModel() {
    private val _name = MutableStateFlow("Yam")
    val name = _name.asStateFlow()

    fun fetchCustomerDetails(customerId: Int) {
        viewModelScope.launch {
            try {
                val params = mapOf("userId" to "$customerId")
                val jsonResponse =
                    NetworkUtils.makeRequest("fetchUserName", KliqueHttpMethod.GET, params).second

                val responseObject = JSONObject(jsonResponse)
                if (responseObject.has("name")) {
                    _name.value = responseObject.getString("name")
                } else {
                    val error = responseObject.optString("error", "Unknown error occurred")
                    Log.e("UserDetailsError", "Error fetching customer details: $error")
                }

            } catch (e: Exception) {
                Log.e("UserDetailsError", "Exception in fetching customer details: ${e.message}")
            }
        }
    }

    suspend fun searchUsers(query: String): List<SearchUser> {
        val queryString = mapOf("query" to query)
        val users = mutableListOf<SearchUser>()
        try {
            val result = NetworkUtils.makeRequest("searchUser", KliqueHttpMethod.GET, queryString)
            if (result.first) {
                withContext(Dispatchers.IO) {
                    try {
                        Logger.d("KliqueSearch", "Success: ${result.second}")
                        val jsonArray = JSONArray(result.second)
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val userId = jsonObject.optInt("userId", -1)
                            val userAlias = jsonObject.optString("userAlias", "Unknown")
                            val profilePictureUrl = NetworkUtils.fixLocalHostUrl(
                                jsonObject.optString(
                                    "profilePictureUrl",
                                    ""
                                )
                            )
                            val isVerified = jsonObject.optBoolean("isVerified", false)
                            val user = SearchUser(
                                userId = userId,
                                userAlias = userAlias,
                                profilePictureUrl = profilePictureUrl,
                                isVerified = isVerified
                            )
                            users.add(user)
                            Logger.d("KliqueSearch", "Success again: $users")
                        }
                    } catch (e: Exception) {
                        Logger.d("KliqueSearch", "Error: $e")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            Logger.d("search exception", e.toString())
        }
        return users
    }
}

data class SearchUser(
    val userId: Int,
    val userAlias: String,
    val profilePictureUrl: String,
    val isVerified: Boolean = false
)