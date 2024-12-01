package com.justself.klique

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TinyProfileDetails(val name: String, val bioText: String, val profileUrl: String, val kcBalance: Int, val phoneNumber: String)
class ProfileViewModel(private val chatScreenViewModel: ChatScreenViewModel) : ViewModel() {
    var profilePictureUrl: String =
        "https://fastly.picsum.photos/id/433/450/300.jpg?hmac=FIWPaQ-to3njMOAmnwI8dg-5TyOzFGi1nVVfEoEERf4"
        private set
    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    private val _tinyProfileDetails = MutableStateFlow(TinyProfileDetails("","","", 0, ""))
    val tinyProfileDetails: StateFlow<TinyProfileDetails> = _tinyProfileDetails

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio

        init {
        viewModelScope.launch {
            tinyProfileDetails.collect {
                _bio.value = _tinyProfileDetails.value.bioText
            }
        }
    }
    override fun onCleared() {
        Log.d("UpdateProfile", "on cleared called")
    }
    fun fetchProfile(){
        Log.d("UpdateProfile", "Called")
        _loaded.value = true
        val params = mapOf("userId" to "${SessionManager.customerId.value}")
        viewModelScope.launch {
            try {
                val response =
                    NetworkUtils.makeRequest("fetchUserDetails", KliqueHttpMethod.GET, params)
                if (response.first) {
                    val jsonObject = JSONObject(response.second)
                    val bioText = jsonObject.getString("bio")
                    val profilePicture = jsonObject.getString("profileUrl").replace("127.0.0.1", "10.0.2.2")
                    val tinyDetails = TinyProfileDetails(
                        "", bioText, profilePicture, 0, ""
                    )

                    withContext(Dispatchers.Main) {
                        _tinyProfileDetails.value = tinyDetails
                    }
                    Log.d("Profile", _tinyProfileDetails.value.toString())

                }
            } catch (e: Exception) {
                Log.d("Profile", e.toString())
            }
        }
    }

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
        if (newBio != _tinyProfileDetails.value.bioText) {
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
    fun updateBio(newBio: String) {
        _bio.value = newBio // Allow independent updates to bio
    }

    fun setTinyProfileDetails(details: TinyProfileDetails) {
        _tinyProfileDetails.value = details
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