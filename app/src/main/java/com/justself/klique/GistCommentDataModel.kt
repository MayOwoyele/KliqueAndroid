package com.justself.klique

data class GistComment(val id: Int, val fullName: String, val comment: String, val customerId: Int, val replies: List<Reply>)
data class Reply(val id: Int, val fullName: String, val customerId: Int, val reply: String)