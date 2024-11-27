package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import android.graphics.Color as AndroidColor

class BioViewModel(private val contactsRepository: ContactsRepository) : ViewModel() {
    private val _postComments = MutableStateFlow<List<StatusComments>>(emptyList())
    val postComments = _postComments.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()
    fun fetchProfile(bioUserId: Int, spectatorUserId: Int) {
        val params = mapOf(
            "bioUserId" to "$bioUserId",
            "spectatorUserId" to "$spectatorUserId"
        )
        viewModelScope.launch {
            try {
                val response =
                    NetworkUtils.makeRequest("fetchProfile", KliqueHttpMethod.GET, params)
                if (response.first) {
                    val jsonResponse = response.second
                    val jsonObject = JSONObject(jsonResponse)

                    val profileAssigned = Profile(
                        customerId = jsonObject.getInt("userId"),
                        bioImage = jsonObject.getString("bioImage"),
                        backgroundColor = colorFromHex(jsonObject.getString("backgroundColor")),
                        fullName = jsonObject.getString("fullName"),
                        bioText = jsonObject.getString("bioText"),
                        posts = parsePosts(jsonObject.getJSONArray("posts")),
                        classSection = jsonObject.getString("classSection"),
                        isSpectator = jsonObject.getBoolean("isSpectator"),
                        seatedCount = jsonObject.getInt("seatedCount"),
                        isVerified = jsonObject.getBoolean("isVerified")
                    )
                    Log.d("ProfileJson", profileAssigned.toString())
                    _profile.value = profileAssigned
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parsePosts(postsArray: JSONArray): List<Post> {
        val posts = mutableListOf<Post>()
        for (i in 0 until postsArray.length()) {
            val postJson = postsArray.getJSONObject(i)
            val post = Post(
                id = postJson.getString("id"),
                type = postJson.getString("type"),
                content = postJson.getString("content"),
                thumbnail = postJson.optString("thumbnail", ""),
                topComments = parseComments(postJson.getJSONArray("topComments")),  // Parse top comments
                totalComments = postJson.getInt("totalComments"),
                kcLikesCount = postJson.getInt("kcLikesCount")
            )
            posts.add(post)
        }
        return posts
    }

    private fun parseComments(commentsArray: JSONArray): List<StatusComments> {
        val comments = mutableListOf<StatusComments>()
        for (i in 0 until commentsArray.length()) {
            val commentJson = commentsArray.getJSONObject(i)
            val comment = StatusComments(
                name = commentJson.getString("name"),
                customerId = commentJson.getInt("customerId"),
                text = commentJson.getString("text")
            )
            comments.add(comment)
        }
        return comments
    }

    private fun colorFromHex(hex: String): Color {
        val androidColorInt = AndroidColor.parseColor(hex)
        return Color(androidColorInt)
    }

    fun leaveSeat(enemyId: Int, customerId: Int) {
        val json = """
            {
            "enemyId": $enemyId,
            "userId": $customerId
            }
        """.trimIndent()
        viewModelScope.launch {
            try {
                val response = NetworkUtils.makeRequest(
                    "leaveSeat",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody = json
                )
                if (response.first){
                    _profile.value = _profile.value?.copy(isSpectator = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun takeSeat(enemyId: Int, customerId: Int) {
        val json = """
            {
            "enemyId": $enemyId,
            "userId": $customerId
            }
        """.trimIndent()
        viewModelScope.launch {
            try {
                val response = NetworkUtils.makeRequest(
                    "takeSeat",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody = json
                )
                if (response.first){
                    Log.d("Response", "response successful")
                    _profile.value = _profile.value?.copy(isSpectator = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkIfContact(enemyId: Int): LiveData<String?> = liveData {
        val contact = contactsRepository.getContactByCustomerId(enemyId)
        emit(contact?.name)
    }

    fun formatSpectatorCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.US, "%dK", count / 1_000)
            else -> count.toString()
        }
    }

    fun sendComment(comment: String, customerId: Int) {
        viewModelScope.launch {
            try {
                val response = NetworkUtils.makeRequest(
                    "sendStatusComment",
                    KliqueHttpMethod.POST,
                    params = emptyMap(),
                    jsonBody = comment
                )
                if (response.first) {
                    val newComment = StatusComments(
                        name = "You",
                        customerId = customerId,
                        text = comment
                    )
                    val updatedComments = _postComments.value.toMutableList()
                    updatedComments.add(newComment)
                    _postComments.value = updatedComments
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchPostComments(postId: String) {
        val params = mapOf("postId" to postId)
        try {
            viewModelScope.launch {
                val response =
                    NetworkUtils.makeRequest("fetchStatusPostComments", KliqueHttpMethod.GET, params)
                if (response.first) {
                    val jsonResponse = response.second
                    val jsonObject = JSONObject(jsonResponse)
                    val commentsArray = jsonObject.getJSONArray("comments")
                    val commentsList = mutableListOf<StatusComments>()

                    for (i in 0 until commentsArray.length()) {
                        val commentJson = commentsArray.getJSONObject(i)
                        val statusComment = StatusComments(
                            name = commentJson.getString("name"),
                            customerId = commentJson.getInt("customer_id"),
                            text = commentJson.getString("text")
                        )
                        commentsList.add(statusComment)
                    }
                    _postComments.value = commentsList

                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
}

class BioViewModelFactory(
    private val contactsRepository: ContactsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BioViewModel(contactsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}