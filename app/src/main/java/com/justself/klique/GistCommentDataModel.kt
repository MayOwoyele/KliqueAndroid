package com.justself.klique

data class GistComment(
    val id: String,
    val fullName: String,
    val comment: String,
    val customerId: Int,
    val replies: List<Reply>,
    val upVotes: Int,
    val upVotedByYou: Boolean = false
)

data class Reply(val id: String, val fullName: String, val customerId: Int, val reply: String)