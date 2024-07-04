// File: WebSocketListener.kt
package com.justself.klique

import org.json.JSONObject

interface WebSocketListener {
    val listenerId: String
    fun onMessageReceived(type: String, jsonObject: JSONObject)
}