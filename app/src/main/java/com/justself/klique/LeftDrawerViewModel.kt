package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LeftDrawerViewModel : ViewModel() {
    private val _tinyProfileDetails = MutableStateFlow(TinyProfileDetails("", "", "", 0, ""))
    val tinyProfileDetails: StateFlow<TinyProfileDetails> = _tinyProfileDetails

    init {
        fetchProfile()
    }
    fun fetchProfile() {
        Log.d("UpdateProfile", "Called")
        val params = mapOf("userId" to "${SessionManager.customerId.value}")
        viewModelScope.launch {
            try {
                val response: suspend() -> jwtHandle =
                    { NetworkUtils.makeJwtRequest("fetchUserDetails", KliqueHttpMethod.GET, params) }
                val action: suspend( jwtHandle) -> Unit = {triple ->
                    val jsonObject = JSONObject(triple.second)
                    val bioText = jsonObject.getString("bio")
                    val profilePicture =
                        jsonObject.getString("profileUrl").replace("127.0.0.1", "10.0.2.2")
                    val name = jsonObject.getString("name")
                    val kcBalance = jsonObject.getInt("kcBalance")
                    val phoneNumber = jsonObject.getString("phoneNumber")
                    val tinyDetails = TinyProfileDetails(
                        name, bioText, profilePicture, kcBalance, phoneNumber
                    )
                    withContext(Dispatchers.Main) {
                        _tinyProfileDetails.value = tinyDetails
                    }
                    Log.d("LeftViewModel", _tinyProfileDetails.value.toString())
                }
                val error: suspend (jwtHandle) -> Unit = {}
                JWTNetworkCaller.performReusableNetworkCalls(response, action, error)
            } catch (e: Exception) {
                Log.d("LeftViewModel", "${e}")
            }
        }
    }
}
typealias jwtHandle = Triple<Boolean, String, Int>