package com.justself.klique

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ProfileViewModel(private val chatScreenViewModel: ChatScreenViewModel) : ViewModel() {
    var profilePictureUrl: String =
        "https://fastly.picsum.photos/id/433/450/300.jpg?hmac=FIWPaQ-to3njMOAmnwI8dg-5TyOzFGi1nVVfEoEERf4"
        private set
    var bio: String = "This is the user's bio."
        private set

    fun updateProfile(
        newProfilePictureUri: Uri?,
        newBio: String,
        averageColor: String,
        context: Context,
        customerId: Int
    ) {
        if (newProfilePictureUri != null) {
            Log.d("Update Profile", "Uri is $newProfilePictureUri")
            viewModelScope.launch(Dispatchers.IO) {
                val chatParticipantIds = chatScreenViewModel.fetchRelevantIds()
                val byteArray = FileUtils.loadFileAsByteArray(context, newProfilePictureUri)
                if (byteArray != null) {
                    sendProfilePictureUpdateToServer(byteArray, chatParticipantIds, averageColor, customerId)
                } else {
                    Log.d("Profile Picture", "Byte array null")
                }
            }
        }
        if (newBio != bio) {
            Log.d("Update Profile", "Bio is called?")
            updateBio(newBio, customerId)
        }
    }

    private fun sendProfilePictureUpdateToServer(
        byteArray: ByteArray,
        chatParticipantIds: List<Int>,
        averageColor: String,
        customerId: Int
    ) {
        val uploadJsonObject = JSONObject().apply {
            put("averageColor", averageColor)
            put("userId", customerId)
            put("chatParticipantIds", JSONArray(chatParticipantIds))
        }.toString()
        val participantJson = JSONObject().apply {
            put("type", "chatListArray")
            put("chatParticipantIds", JSONArray(chatParticipantIds))
        }.toString()
        try {
            viewModelScope.launch {
                val response = NetworkUtils.makeRequest(
                    "uploadBioImage",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    binaryBody = byteArray,
                    jsonBody = uploadJsonObject
                )
                if (response.first) {
                    WebSocketManager.send(participantJson)
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun updateBio(text: String, customerId: Int) {
        val bioJson = JSONObject().apply {
            put("userId", customerId)
            put("text", text)
        }.toString()
        try {
            viewModelScope.launch {
                NetworkUtils.makeRequest("updateBioText", KliqueHttpMethod.POST, emptyMap(), jsonBody = bioJson)
            }
        } catch (e:Exception){
            e.printStackTrace()
        }
    }
}

class ProfileViewModelFactory(
    private val chatScreenViewModel: ChatScreenViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ProfileViewModel(chatScreenViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}