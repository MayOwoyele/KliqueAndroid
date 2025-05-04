package com.justself.klique

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object ChatVMObject {
    private val _messagesArray = MutableStateFlow(
        Triple(false, "string", 0)
    )
    val messagesArray = _messagesArray.asStateFlow()
    fun callFetch(){
        CoroutineScope(Dispatchers.IO).launch {
            fetchUndeliveredMessages {responseTriple ->
                _messagesArray.value = responseTriple.toNetworkTriple()
                loggerD("fetchUndelivered"){ _messagesArray.value.second}
            }
        }
    }
    suspend fun fetchUndeliveredMessages(actionAction: suspend (NetworkUtils.JwtTriple) -> Unit) {
        NetworkUtils.makeJwtRequest(
            endpoint = "fetchUndeliveredMessages",
            method = KliqueHttpMethod.GET,
            params = emptyMap(),
            action = { responseTriple ->
                actionAction(responseTriple)
            },
            errorAction = { errorTriple ->
                loggerD("fetchUndeliveredMessages", {"Error: ${errorTriple.toNetworkTriple().second}"})
            }
        )
    }
    suspend fun acknowledgeMessages(messageIds: List<String>) {
        loggerD("AckMessages") { "sending" }
        val jsonBody = JSONObject().apply {
            put("messageIds", JSONArray(messageIds))
        }.toString()
        val action: suspend (NetworkUtils.JwtTriple) -> Unit = { responseTriple ->
            if (responseTriple is NetworkUtils.JwtTriple.Value) {
                loggerD("AckMessages") { "Response code: ${responseTriple.responseCode}" }
            }
        }
        val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {}
        NetworkUtils.makeJwtRequest(
            endpoint = "acknowledgeMessages",
            method = KliqueHttpMethod.POST,
            params = emptyMap(),
            jsonBody = jsonBody,
            action = action,
            errorAction = errorAction
        )
    }
}