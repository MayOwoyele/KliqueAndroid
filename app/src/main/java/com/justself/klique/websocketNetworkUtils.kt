// File: WebSocketManager.kt
package com.justself.klique

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.ByteString
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.MDC.put
import java.net.URI
import java.net.URLEncoder
import java.nio.ByteBuffer
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate


object WebSocketManager {
    private var webSocketClient: WebSocketClient? = null
    private val sslContext: SSLContext = SSLContext.getInstance("SSL").apply {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        init(null, trustAllCerts, SecureRandom())
    }

    private val listeners = mutableMapOf<String, WebSocketListener>()

    fun registerListener(listener: WebSocketListener) {
        listeners[listener.listenerId] = listener
    }

    fun unregisterListener(listener: WebSocketListener) {
        listeners.remove(listener.listenerId)
    }

    private fun createWebSocketClient(url: URI): WebSocketClient {
        return object : WebSocketClient(url) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connected!")
            }

            override fun onMessage(message: String) {
                Log.d("WebSocket", message)
                println("Received message: $message")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val jsonObject = JSONObject(message)
                        val type = jsonObject.getString("type")
                        val targetId = jsonObject.optString("targetId", "")

                        println("Routing text message: Type: $type, TargetId: $targetId")
                        routeMessageToViewModel(type, targetId, jsonObject)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        println("JSONException occurred while parsing message: ${e.message}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Unexpected error occurred while processing message: ${e.message}")
                    }
                }
            }

            override fun onMessage(bytes: ByteBuffer) {
                println("Received binary message")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val prefixBytes = ByteArray(12) // Adjust the prefix length as needed
                        bytes.get(prefixBytes)
                        val prefix = String(prefixBytes, Charsets.UTF_8).trim()
                        val binaryData = ByteArray(bytes.remaining())
                        bytes.get(binaryData)

                        val jsonObject = JSONObject()
                        jsonObject.put("prefix", prefix)
                        jsonObject.put("binaryData", Base64.encodeToString(binaryData, Base64.DEFAULT))

                        println("Routing binary message: Prefix: $prefix")
                        routeBinaryMessageToViewModel(prefix, jsonObject)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        println("JSONException occurred while parsing binary message: ${e.message}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Unexpected error occurred while processing binary message: ${e.message}")
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
        try {
            if (webSocketClient?.isOpen == true) {
                webSocketClient?.send(message)
            } else {
                throw WebsocketNotConnectedException()
            }
        } catch (e: WebsocketNotConnectedException) {
            Log.e("WebSocketManager", "WebSocket not connected: ${e.message}")
            // Optionally handle reconnection or notify the user
        } catch (e: Exception) {
            Log.e("WebSocketManager", "Exception in sending message: ${e.message}")
        }
    }

    fun sendBinary(message: ByteArray) {
        try {
            if (webSocketClient?.isOpen == true) {
                webSocketClient?.send(message)
            } else {
                throw WebsocketNotConnectedException()
            }
        } catch (e: WebsocketNotConnectedException) {
            Log.e("WebSocketManager", "WebSocket not connected: ${e.message}")
            // Optionally handle reconnection or notify the user
        } catch (e: Exception) {
            Log.e("WebSocketManager", "Exception in sending binary data: ${e.message}")
        }
    }

    fun close() {
        webSocketClient?.close()
    }

    private fun routeBinaryMessageToViewModel(prefix: String, jsonObject: JSONObject) {
        val parts = prefix.split(":")
        if (parts.size == 2) {
            val type = parts[0].lowercase()
            val targetId = parts[1].trim()
            val listenerId = when (type) {
                "KImage", "KVideo", "KAudio" -> "SharedCliqueViewModel"
                else -> targetId
            }

            listenerId.let {
                listeners[it]?.onMessageReceived(type, jsonObject)
                println("Binary message routed to listener: $listenerId, Type: $type, TargetId: $targetId")
            } ?: println("Unhandled binary data type or target ID: $type, $targetId")
        } else {
            println("Invalid prefix format: $prefix")
        }
    }

    private fun routeMessageToViewModel(type: String, targetId: String, jsonObject: JSONObject) {
        val listenerId = when (type) {
            "gistCreated", "previousMessages", "message", "messageAck" -> "SharedCliqueViewModel"
            else -> targetId
        }

        println("Routing text message to listener: $listenerId, Type: $type, TargetId: $targetId")
        println("Raw JSON data from server: $jsonObject")

        listenerId.let {
            listeners[it]?.onMessageReceived(type, jsonObject)?.also {
                println("Text message routed to listener: $listenerId, Type: $type, TargetId: $targetId")
            } ?: println("Unhandled message type or target ID: $type, $targetId")
        }
    }
    fun simulateWebSocketMessages() {
        // Generate 10 messages from different people, including customerId 25
        val customerIds = listOf(1, 2, 3, 4, 5, 25, 7, 8, 9, 10) // Including customerId 25
        val messagesJsonArray = customerIds.map { customerId ->
            """
        {
            "id": ${System.currentTimeMillis()},
            "gistId": "someGistId",
            "customerId": $customerId,
            "fullName": "User $customerId",
            "content": "Hello from User $customerId",
            "status": "sent",
            "messageType": "text"
        }
        """
        }

        // Combine all messages into a single JSON object
        val combinedMessage = """
    {
        "type": "previousMessages",
        "gistId": "someGistId",
        "messages": [${messagesJsonArray.joinToString(",")}]
    }
    """

        // Simulate receiving the combined message
        webSocketClient?.onMessage(combinedMessage)
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