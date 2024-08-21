package com.justself.klique

import android.content.Context
import android.net.Network
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ProfileViewModel(private val chatScreenViewModel: ChatScreenViewModel) : ViewModel() {
    var profilePictureUrl: String =
        "https://fastly.picsum.photos/id/433/450/300.jpg?hmac=FIWPaQ-to3njMOAmnwI8dg-5TyOzFGi1nVVfEoEERf4"
        private set
    var bio: String = "This is the user's bio."
        private set

    fun updateProfile(newProfilePictureUri: Uri?, newBio: String, averageColor: String, context: Context) {
        if (newProfilePictureUri != null) {
            Log.d("Update Profile", "Uri is $newProfilePictureUri")
            viewModelScope.launch(Dispatchers.IO) {
                val chatParticipantIds = chatScreenViewModel.fetchRelevantIds()
                val byteArray = FileUtils.loadFileAsByteArray(context, newProfilePictureUri)
                if (byteArray != null) {
                    sendProfileUpdateToServer(byteArray, chatParticipantIds, averageColor)
                } else {
                    Log.d("Profile Picture", "Byte array null")
                }
            }
        }
        if (newBio != bio) {
            Log.d("Update Profile", "Bio is called?")
        }
    }

    private fun sendProfileUpdateToServer(byteArray: ByteArray, chatParticipantIds: List<Int>, averageColor: String) {
        // Convert chatParticipantIds to JSON and then to ByteArray
        val chatParticipantIdsJson = chatParticipantIds.joinToString(prefix = "[", postfix = "]") { it.toString() }
        val chatIdsByteArray = chatParticipantIdsJson.toByteArray(Charsets.UTF_8)

        // Convert averageColor to ByteArray
        val colorByteArray = averageColor.toByteArray(Charsets.UTF_8)

        // Combine lengths and actual data
        val metadataLength = chatIdsByteArray.size + colorByteArray.size

        val metadataByteArray = ByteBuffer.allocate(4 + metadataLength)
            .putInt(chatIdsByteArray.size)  // Store the length of chatIdsByteArray
            .put(chatIdsByteArray)          // Store the chatParticipantIds byte array
            .put(colorByteArray)            // Store the color byte array
            .array()

        // Combine metadata and image data
        val finalByteArray = ByteBuffer.allocate(metadataByteArray.size + byteArray.size)
            .put(metadataByteArray)  // Add metadata
            .put(byteArray)          // Add image data
            .array()

        // Now you can send finalByteArray to your WebSocket
        WebSocketManager.sendBinary(finalByteArray)
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