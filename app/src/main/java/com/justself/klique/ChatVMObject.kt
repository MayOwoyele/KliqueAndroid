package com.justself.klique

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ChatVMObject {
    // Initialize messagesArray as an empty JSONArray.
    private val _messagesArray = MutableStateFlow(
        Triple(true, "string", 0)
    )
    val messagesArray = _messagesArray.asStateFlow()
    fun callFetch(){
        CoroutineScope(Dispatchers.IO).launch {
            fetchUndeliveredMessages {responseTriple ->
                _messagesArray.value = responseTriple
            }
        }
    }
    suspend fun fetchUndeliveredMessages(actionAction: suspend (networkTriple) -> Unit) {
        JWTNetworkCaller.performReusableNetworkCalls(
            response = {
                NetworkUtils.makeJwtRequest(
                    endpoint = "fetchUndeliveredMessages",
                    method = KliqueHttpMethod.GET,
                    params = mapOf("userId" to SessionManager.customerId.value.toString())
                )
            },
            action = { responseTriple ->
                actionAction(responseTriple)
            },
            errorAction = { errorTriple ->
                Log.e("FetchUndelivered", "Failed to fetch messages: ${errorTriple.third}")
            }
        )
    }
}