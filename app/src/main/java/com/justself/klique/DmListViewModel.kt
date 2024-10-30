package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class DmItem(
    val imageLink: String,
    val fullName: String,
    val enemyId: Int,
    val lastMessage: LastMessage
)

sealed class LastMessage {
    data class Text(val content: String) : LastMessage()
    object Photo : LastMessage()
}

class DmListViewModel : ViewModel() {
    private val _dmList = MutableStateFlow<List<DmItem>>(emptyList())
    val dmList: StateFlow<List<DmItem>> = _dmList

    fun fetchDmList(customerId: Int) {
        viewModelScope.launch {
            val params = mapOf("userId" to "$customerId")
            val response = NetworkUtils.makeRequest("fetchDmList", KliqueHttpMethod.GET, params)
            if (response.first) {
                try {
                    val jsonArray = JSONArray(response.second)
                    val dmItems = (0 until jsonArray.length()).map { i ->
                        val jsonObject = jsonArray.getJSONObject(i)

                        val imageLink = jsonObject.getString("imageLink")
                        val fullName = jsonObject.getString("fullName")
                        val enemyId = jsonObject.getInt("enemyId")

                        val lastMessageType = jsonObject.getString("lastMessageType")
                        val lastMessage = when (lastMessageType) {
                            "Text" -> LastMessage.Text(jsonObject.getString("lastMessageContent"))
                            "Photo" -> LastMessage.Photo
                            else -> LastMessage.Text("")
                        }

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
            } else {
                Log.e("fetchDmList", "Request failed: ${response.second}")
            }
        }
    }
}