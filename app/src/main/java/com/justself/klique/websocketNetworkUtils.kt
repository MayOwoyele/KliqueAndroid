package com.justself.klique

import android.util.Log
import java.net.URI
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.security.SecureRandom
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URLEncoder
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject

class WebSocketManager(private val viewModel: SharedCliqueViewModel) {
    private var webSocketClient: WebSocketClient? = null
    private val sslContext: SSLContext = SSLContext.getInstance("SSL").apply {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        init(null, trustAllCerts, SecureRandom())
    }

    private fun createWebSocketClient(url: URI): WebSocketClient {
        return object : WebSocketClient(url) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connected!")
            }

            override fun onMessage(message: String) {
                println("Received message: $message")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val jsonObject = JSONObject(message)
                        when (jsonObject.getString("type")) {
                            "status" -> {
                                val data = jsonObject.getJSONObject("data")
                                val customerId = data.getInt("customerId")
                                val fullName = data.getString("fullName")
                                Log.i("WebSocketManager", "Connected: $customerId, $fullName")
                            }
                            "gistCreated" -> {
                                val gistId = jsonObject.getInt("gistId")
                                val topic = jsonObject.getString("topic")
                                viewModel.setGistCreatedOrJoined(topic, gistId)
                                Log.i("WebSocketManager", "Gist Created: $gistId, Topic: $topic")
                            }
                            "previousMessages" -> {
                                val gistId = jsonObject.getInt("gistId")
                                val messagesJsonArray = jsonObject.getJSONArray("messages")
                                val messages = (0 until messagesJsonArray.length()).map { i ->
                                    val msg = messagesJsonArray.getJSONObject(i)
                                    ChatMessage(
                                        id = msg.getInt("id"),
                                        gistId = msg.getInt("gistId"),
                                        customerId = msg.getInt("customerId"),
                                        sender = msg.getString("fullName"),
                                        content = deEscapeContent(msg.getString("content")),
                                        status = msg.getString("status"),
                                        messageType = msg.getString("messageType")
                                    )
                                }
                                viewModel.handlePreviousMessages(gistId, messages)
                            }
                            "message" -> {
                                val gistId = jsonObject.getInt("gistId")
                                val messageId = jsonObject.getInt("id")
                                val customerId = jsonObject.getInt("customerId")
                                val fullName = jsonObject.getString("fullName")
                                val content = deEscapeContent(jsonObject.getString("content"))
                                viewModel.addMessage(ChatMessage(id = messageId, gistId = gistId, customerId = customerId, sender = fullName, content = content, status = "received"))
                            }
                            "messageAck" -> {
                                val gistId = jsonObject.getInt("gistId")
                                val messageId = jsonObject.getInt("id")
                                val ackMessage = jsonObject.getString("message")
                                Log.i("WebSocketManager", "Message acknowledged: $ackMessage")
                                viewModel.messageAcknowledged(gistId, messageId)
                            }
                            "image" -> {
                                val gistId = jsonObject.getInt("gistId")
                                val messageId = jsonObject.getInt("id")
                                val customerId = jsonObject.getInt("customerId")
                                val fullName = jsonObject.getString("fullName")
                                val base64Image = jsonObject.getString("content")
                                viewModel.addMessage(ChatMessage(id = messageId, gistId = gistId, customerId = customerId, sender = fullName, content = base64Image, status = "received", messageType = "image"))
                                }
                            "imageAck" -> {
                                val gistId = jsonObject.getInt("gistId")
                                val messageId = jsonObject.getInt("id")
                                val ackMessage = jsonObject.getString("message")
                                Log.i("WebSocketManager", "Image acknowledged: $ackMessage")
                                viewModel.messageAcknowledged(gistId, messageId, "image")
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket closed: Reason - $reason")
            }

            override fun onError(ex: Exception) {
                println("WebSocket error: ${ex.message}")
            }
        }.apply {
            if (url.scheme.equals("wss")) {
                setSocketFactory(sslContext.socketFactory)
            }
        }
    }

    fun connect(url: String, customerId: Int, fullName: String) {
        val encodedFullName = URLEncoder.encode(fullName, "UTF-8")
        val uri = URI("$url?customerId=$customerId&fullName=$encodedFullName")

        Log.i("WebSocketManager", "Connecting to URI: $uri")
        webSocketClient = createWebSocketClient(uri).apply {
            connect()
        }
    }

    fun send(message: String) {
        webSocketClient?.send(message)
    }

    fun close() {
        webSocketClient?.close()
    }
}

fun deEscapeContent(escapedContent: String): String {
    return escapedContent.replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\b", "\b")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}