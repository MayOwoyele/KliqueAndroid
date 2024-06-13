package com.justself.klique

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class CommentsViewModel : ViewModel() {

    private val networkUtils = NetworkUtils

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchComments(productId: Int) {
        viewModelScope.launch {
            try {
                val params = mapOf(
                    "action" to "fetchComments",
                    "productId" to productId.toString()
                )
                val response = NetworkUtils.makeRequest(
                    endpoint = "api.php",
                    method = "GET",
                    params = params
                )
                val commentsList = Json.decodeFromString<List<Comment>>(response)
                _comments.postValue(commentsList)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Unknown error")
            }
        }
    }
    fun addComment(newComment: NewComment) {
        if (newComment.comment.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val (response, statusCode) = NetworkUtils.makeRequestWithStatusCode(
                        endpoint = "api.php",
                        params = mapOf(
                            "action" to "addComment",
                            "productId" to newComment.productId.toString(),
                            "customerId" to newComment.customerId.toString(),
                            "comment" to newComment.comment
                        )
                    )
                    if (statusCode == 201) { // Using status code to check success
                        refreshComments(newComment.productId)  // Refresh the comments to include the new one
                    } else {
                        _error.postValue("Failed to post comment, status code: $statusCode")
                    }
                } catch (e: Exception) {
                    _error.postValue("Failed to post comment: ${e.message}")
                }
            }
        } else {
            _error.postValue("Comment cannot be empty")
        }
    }

    private fun refreshComments(productId: Int) {
        fetchComments(productId)
    }

}