package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class DmItem(
    val imageLink: String,
    val fullName: String,
    val enemyId: Int,
    val lastMessage: LastMessage
)

sealed class LastMessage {
    data class Text(val content: String) : LastMessage()
    data object Photo : LastMessage()
}

class DmListViewModel : ViewModel() {
    private val _dmList = MutableStateFlow<List<DmItem>>(emptyList())
    val dmList: StateFlow<List<DmItem>> = _dmList

    fun fetchDmList(customerId: Int) {
        viewModelScope.launch {
            try {
                val params = mapOf("userId" to "$customerId")
                NetworkUtils.makeJwtRequest("fetchDmList", KliqueHttpMethod.GET, params,
                    action = { response ->
                        try {
                            val jsonArray = JSONArray(response.toNetworkTriple().second)
                            Log.d("Parsing", response.toNetworkTriple().second)
                            val dmItems = (0 until jsonArray.length()).map { i ->
                                val jsonObject = jsonArray.getJSONObject(i)

                                val imageLink =
                                    NetworkUtils.fixLocalHostUrl(jsonObject.getString("imageLink"))
                                val fullName = jsonObject.getString("fullName")
                                val enemyId = jsonObject.getInt("enemyId")

                                val lastMessageType = jsonObject.getString("lastMessageType")
                                val lastMessage = when (lastMessageType) {
                                    "DText" -> LastMessage.Text(jsonObject.getString("lastMessageContent"))
                                    "DImage" -> LastMessage.Photo
                                    else -> LastMessage.Text("")
                                }
                                Log.e("fetchDmList", "image link: $imageLink")

                                DmItem(
                                    imageLink = imageLink,
                                    fullName = fullName,
                                    enemyId = enemyId,
                                    lastMessage = lastMessage
                                )
                            }
                            _dmList.value = dmItems
                        } catch (e: Exception) {
                            Log.e("fetchDmList", "Error parsing response: ${e.message}")
                        }
                    },
                    errorAction = { response ->
                        Log.e("fetchDmList", "Request failed: ${response.toNetworkTriple().second}")
                    }
                )
            } catch (e: Exception) {
                Log.e("fetchDmList", "Exception: ${e.message}", e)
            }
        }
    }
}