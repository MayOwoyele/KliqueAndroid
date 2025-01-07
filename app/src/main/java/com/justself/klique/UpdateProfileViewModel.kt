package com.justself.klique

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.justself.klique.JWTNetworkCaller.performReusableNetworkCalls
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TinyProfileDetails(
    val name: String,
    val bioText: String,
    val profileUrl: String,
    val kcBalance: Int,
    val phoneNumber: String
)

class ProfileViewModel(private val chatScreenViewModel: ChatScreenViewModel) : ViewModel() {
    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    private val _tinyProfileDetails = MutableStateFlow(TinyProfileDetails("", "", "", 0, ""))
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

    fun fetchProfile() {
        Log.d("UpdateProfile", "Called")
        val params = mapOf("userId" to "${SessionManager.customerId.value}")
        viewModelScope.launch {
            try {
                val response: suspend() -> jwtHandle =
                    {
                        NetworkUtils.makeJwtRequest(
                            "fetchUserDetails",
                            KliqueHttpMethod.GET,
                            params
                        )
                    }
                val action: suspend(jwtHandle) -> Unit ={triple ->
                    val jsonObject = JSONObject(triple.second)
                    val bioText = jsonObject.getString("bio")
                    val profilePicture =
                        NetworkUtils.fixLocalHostUrl(jsonObject.getString("profileUrl"))
                    val tinyDetails = TinyProfileDetails(
                        "", bioText, profilePicture, 0, ""
                    )

                    withContext(Dispatchers.Main) {
                        _tinyProfileDetails.value = tinyDetails
                        _loaded.value = true
                    }
                    Log.d("Profile", _tinyProfileDetails.value.toString())

                }
                val error: suspend (jwtHandle) -> Unit = {}
                JWTNetworkCaller.performReusableNetworkCalls(response, action, error)
            } catch (e: Exception) {
                Log.d("Profile", e.toString())
            }
        }
    }

    fun updateProfile(
        newProfilePictureUri: Uri?,
        newBio: String,
        context: Context,
        customerId: Int
    ) {
        if (newProfilePictureUri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val chatParticipantIds = chatScreenViewModel.fetchRelevantIds()
                val byteArray = FileUtils.loadFileAsByteArray(context, newProfilePictureUri)
                _loaded.value = false
                if (byteArray != null) {
                    sendProfilePictureUpdateToServer(
                        byteArray,
                        chatParticipantIds,
                        customerId
                    )
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
        customerId: Int
    ) {
        val uploadFields = listOf(
            MultipartField(
                name = "userId",
                value = customerId.toString()
            ),
            MultipartField(
                name = "chatParticipantIds",
                value = chatParticipantIds.joinToString(separator = ",")
            ),
            MultipartField(
                name = "image",
                value = byteArray,
                fileName = "profile_picture.jpg",
                mimeType = MimeType.IMAGE_JPEG
            )
        )

        viewModelScope.launch {
            try {
                _loaded.value = false
                performReusableNetworkCalls(
                    response = {
                        NetworkUtils.makeMultipartRequest("uploadBioImage", uploadFields)
                    },
                    action = {
                        _loaded.value = true
                        Toast.makeText(
                            appContext,
                            "Successfully updated your image",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    errorAction = { response ->
                        _loaded.value = true
                        Toast.makeText(
                            appContext,
                            "Failed to update your image. ${response.second}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("UpdateProfile", "Error: ${response.third} - ${response.second}")
                    }
                )
            } catch (e: Exception) {
                Log.e("UpdateProfile", "Exception: ${e.message}", e)
            }
        }
    }

    private fun updateBio(text: String, customerId: Int) {
        val bioJson = JSONObject().apply {
            put("userId", customerId)
            put("text", text)
        }.toString()

        viewModelScope.launch {
            try {
                performReusableNetworkCalls(
                    response = {
                        NetworkUtils.makeJwtRequest(
                            "updateBioText",
                            KliqueHttpMethod.POST,
                            params = emptyMap(),
                            jsonBody = bioJson
                        )
                    },
                    action = {
                        Toast.makeText(
                            appContext,
                            "Successfully updated your bio",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    errorAction = { response ->
                        Toast.makeText(
                            appContext,
                            "Failed to update your bio. ${response.second}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("UpdateBio", "Error: ${response.third} - ${response.second}")
                    }
                )
            } catch (e: Exception) {
                Log.e("UpdateBio", "Exception: ${e.message}", e)
            }
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