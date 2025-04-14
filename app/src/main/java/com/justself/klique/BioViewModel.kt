package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import com.justself.klique.JWTNetworkCaller.performReusableNetworkCalls
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.viewModel.parseGistsFromResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class BioViewModel(private val contactsRepository: ContactsRepository) : ViewModel() {
    private val _postComments = MutableStateFlow<List<StatusComments>>(emptyList())
    val postComments = _postComments.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile = _profile.asStateFlow()
    private val _gistList = MutableStateFlow<List<GistModel>>(emptyList())
    val gistList = _gistList.asStateFlow()
    init {
        WebSocketManager.bioViewModel = this
    }

    override fun onCleared() {
        super.onCleared()
        WebSocketManager.bioViewModel = null
    }

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
                    Log.d("CommentStatus", response.second.toString())
                    val jsonObject = JSONObject(jsonResponse)

                    val profileAssigned = Profile(
                        customerId = jsonObject.getInt("userId"),
                        bioImage = NetworkUtils.fixLocalHostUrl(jsonObject.getString("bioImage")),
                        fullName = jsonObject.getString("fullName"),
                        bioText = jsonObject.getString("bioText"),
                        posts = parsePosts(jsonObject.getJSONArray("posts")),
                        classSection = jsonObject.getString("classSection"),
                        isSpectator = jsonObject.getBoolean("isSpectator"),
                        seatedCount = jsonObject.getInt("seatedCount"),
                        isVerified = jsonObject.getBoolean("isVerified")
                    )
                    _profile.value = profileAssigned
                }
            } catch (e: Exception) {
                Log.d("CommentStatus", e.toString())
            }
        }
    }
    fun fetchMyGists(userId: Int) {
        viewModelScope.launch {
            try {
                val endpoint = "gists/my"
                val method = KliqueHttpMethod.GET
                val params = mapOf("userId" to userId.toString())

                val response = NetworkUtils.makeRequest(
                    endpoint = endpoint,
                    method = method,
                    params = params
                ).second
                val gists = parseGistsFromResponse(response)
                _gistList.value = gists
            } catch (e: Exception) {
                Log.e("fetchGists", "Exception is $e")
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
                content = NetworkUtils.fixLocalHostUrl(postJson.getString("content")),
                thumbnail = postJson.optString("thumbnail", ""),
                topComments = parseComments(postJson.getJSONArray("topComments")),
                totalComments = postJson.getInt("totalComments"),
                kcLikesCount = postJson.getInt("kcLikesCount")
            )
            Log.d("fetchProfile", postJson.getString("content"))
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
                customerId = commentJson.getInt("customer_id"),
                text = commentJson.getString("text")
            )
            comments.add(comment)
        }
        return comments
    }
    fun floatGist(gistId: String) {
        val floatGistId = """
            {
            "type": "floatGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        WebSocketManager.send(BufferObject(WsDataType.BioVM, floatGistId), showToast = true)
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
                NetworkUtils.makeJwtRequest(
                    "leaveSeat",
                    KliqueHttpMethod.POST,
                    params = emptyMap(),
                    jsonBody = json,
                    action = {
                        _profile.value = _profile.value?.copy(
                            isSpectator = false,
                            seatedCount = (_profile.value?.seatedCount ?: 1) - 1
                        )
                    },
                    errorAction = { response ->
                        if (response is NetworkUtils.JwtTriple.Value){
                            Log.e("Response", "Error leaving seat: ${response.response}")
                        }
                    }
                )

            } catch (e: Exception) {
                Log.e("Response", "Exception: ${e.message}", e)
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
                NetworkUtils.makeJwtRequest(
                    "takeSeat",
                    KliqueHttpMethod.POST,
                    params = emptyMap(),
                    jsonBody = json,
                    action = {
                        Log.d("Response", "response successful")
                        _profile.value = _profile.value?.copy(
                            isSpectator = true,
                            seatedCount = (_profile.value?.seatedCount ?: 1) + 1
                        )
                    },
                    errorAction = { response ->
                        if (response is NetworkUtils.JwtTriple.Value){
                            Log.e("Response", "Error taking seat: ${response.response}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("Response", "Exception: ${e.message}", e)
            }
        }
    }

    fun checkIfContact(enemyId: Int): LiveData<String?> = liveData {
        val contact = contactsRepository.getContactByCustomerId(enemyId)
        emit(contact?.name)
    }

    fun formatSpectatorCount(count: Int): String {
        return when {
            count == 0 -> "No"
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.US, "%dK", count / 1_000)
            else -> count.toString()
        }
    }

    fun sendComment(
        comment: String,
        customerId: Int,
        commentText: String,
        replyingTo: String?,
        replyingToId: Int?,
        postId: String
    ) {
        Log.d("CommentStatus", "called")
        viewModelScope.launch {
            try {
                NetworkUtils.makeJwtRequest(
                    "sendStatusComment",
                    KliqueHttpMethod.POST,
                    params = emptyMap(),
                    jsonBody = comment,
                    action = {
                        val newComment = StatusComments(
                            name = "You",
                            customerId = customerId,
                            text = commentText,
                            replyingToId = replyingToId,
                            replyingTo = replyingTo
                        )
                        val updatedComments = _postComments.value.toMutableList().apply {
                            add(newComment)
                        }
                        _postComments.value = updatedComments

                        val updatedPosts = _profile.value?.posts?.map { post ->
                            if (post.id == postId) {
                                post.copy(totalComments = post.totalComments + 1)
                            } else {
                                post
                            }
                        }?.toMutableList() ?: mutableListOf()

                        _profile.value = _profile.value?.copy(posts = updatedPosts)
                    },
                    errorAction = {
                    }
                )
            } catch (e: Exception) {
                Log.d("CommentStatus", "Exception: ${e.message}")
            }
        }
    }

    fun fetchPostComments(postId: String) {
        val params = mapOf("postId" to postId)
        viewModelScope.launch {
            try {
                val response =
                    NetworkUtils.makeRequest(
                        "fetchStatusPostComments",
                        KliqueHttpMethod.GET,
                        params
                    )
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
                            text = commentJson.getString("text"),
                            replyingToId = commentJson.optInt("replying_to_id")
                                .takeIf { it != 0 },
                            replyingTo = commentJson.optString("replying_to")
                                .takeIf { it != "null" }
                        )
                        commentsList.add(statusComment)
                    }
                    _postComments.value = commentsList
                    Log.d("PostComments", "$commentsList")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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