// File: WebSocketManager.kt
package com.justself.klique

import android.content.Context
import android.util.Base64
import android.util.Log
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
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
    var isConnected = false
    private var reconnectionAttempts = 0
    private const val MAX_RECONNECT_ATTEMPTS = 10
    private const val RECONNECT_DELAY = 3000L
    private var shouldReconnect = true
    private var customerId: Int = 0
    private var fullName: String = ""

    private const val PING_INTERVAL = 5000L
    private const val PONG_TIMEOUT = 7000L
    private var lastPongTime = System.currentTimeMillis()
    private var chatScreenViewModel: ChatScreenViewModel? = null
    private var sharedCliqueViewModel: SharedCliqueViewModel? = null
    private var chatRoomViewModel: ChatRoomViewModel? = null
    private var appContext: Context? = null
    private var aReconnection = false

    fun registerListener(listener: WebSocketListener) {
        listeners[listener.listenerId] = listener
    }

    fun unregisterListener(listener: WebSocketListener) {
        listeners.remove(listener.listenerId)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext  // Store the application context
    }

    private fun createWebSocketClient(url: URI, context: Context): WebSocketClient {
        return object : WebSocketClient(url) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connected!")
                isConnected = true
                reconnectionAttempts = 0
                startPingPong()
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)  // 1 second delay
                    chatScreenViewModel?.retryPendingMessages(context)
                    chatScreenViewModel?.currentChat?.value?.let { enemyId ->
                        chatScreenViewModel?.subscribeToOnlineStatus(enemyId)
                    }
                    chatScreenViewModel?.fetchNewMessagesFromServer()
                }
                if (aReconnection){
                    sharedCliqueViewModel?.gistCreatedOrJoined?.value?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            sharedCliqueViewModel?.restoreGistState()
                        }
                        aReconnection = false
                    }
                }
            }

            override fun onWebsocketPong(conn: WebSocket?, f: Framedata?) {
                super.onWebsocketPong(conn, f)
                lastPongTime = System.currentTimeMillis()
                println("Attempting Received Pong")
            }

            override fun onMessage(message: String) {
                Log.d("RawWebSocket", "raw $message")
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
                        jsonObject.put(
                            "binaryData",
                            Base64.encodeToString(binaryData, Base64.DEFAULT)
                        )

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
                isConnected = false
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }

            override fun onError(ex: Exception) {
                println("WebSocket error: ${ex.message}")
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }
        }.apply {
            if (url.scheme.equals("wss")) {
                setSocketFactory(sslContext.socketFactory)
            }
        }
    }

    fun connect(url: String, customerId: Int, fullName: String, context: Context) {
        val encodedFullName = URLEncoder.encode(fullName, "UTF-8")

        // Use URI and URL constructors to build the URL properly
        val uri = URI.create(url).resolve("?customer_id=$customerId&full_name=$encodedFullName")

        this.customerId = customerId
        this.fullName = fullName

        Log.i("RawWebsocket", "Connecting to URI: $uri")
        webSocketClient = createWebSocketClient(uri, context).apply {
            connect()
        }
    }

    fun setChatScreenViewModel(viewModel: ChatScreenViewModel) {
        chatScreenViewModel = viewModel
    }
    fun setSharedCliqueViewModel(viewModel: SharedCliqueViewModel){
        sharedCliqueViewModel = viewModel
    }
    fun setChatRoomViewModel(viewModel: ChatRoomViewModel){
        chatRoomViewModel = viewModel
    }
    fun clearChatRoomViewModel(){
        chatRoomViewModel = null
    }
    private fun scheduleReconnect() {
        aReconnection = true
        if (reconnectionAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectionAttempts++
            println("Attempting to reconnect... (Attempt $reconnectionAttempts)")
            CoroutineScope(Dispatchers.IO).launch {
                delay(RECONNECT_DELAY * reconnectionAttempts)
                if (!isConnected) {
                    appContext?.let {
                        connect(
                            webSocketClient!!.uri.toString(), customerId, fullName,
                            it
                        )
                    }
                }
            }
        } else {
            println("Max reconnection attempts reached. Maintaining steady reconnect interval.")
            CoroutineScope(Dispatchers.IO).launch {
                while (!isConnected && shouldReconnect) {
                    delay(RECONNECT_DELAY * MAX_RECONNECT_ATTEMPTS) // Set to a steady delay interval
                    println("Attempting to reconnect at a steady interval...")
                    appContext?.let {
                        connect(
                            webSocketClient!!.uri.toString(), customerId, fullName,
                            it
                        )
                    }
                }
            }
        }
    }

    private fun startPingPong() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isConnected) {
                try {
                    Log.d("Websocket", "Sending Ping")
                    webSocketClient?.sendPing()
                    delay(PING_INTERVAL)

                    if (System.currentTimeMillis() - lastPongTime > PONG_TIMEOUT) {
                        println("Pong timeout. Reconnecting...")
                        isConnected = false
                        webSocketClient?.close()
                        scheduleReconnect()
                    }
                } catch (e: Exception) {
                    println("Error during ping-pong: ${e.message}")
                    isConnected = false
                    scheduleReconnect()
                }
            }
        }
    }

    fun send(message: String) {
        Log.d("RawWebsocket", "outgoing $message")
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
        Log.d("Websocket", "Type is :$type")
        val listenerId = when (type) {
            "gistCreated", "previousMessages", "exitGist", "gistMessageAck", "gistCreationError", "KText", "KImage", "KVideo", "KAudio", "spectatorUpdate", "olderMessages", "gistRefreshUpdate" -> "SharedCliqueViewModel"
            "is_online", "PText", "ack", "PImage", "PAudio", "PVideo", "PGistInvite" -> "ChatScreenViewModel"
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
        // Generate 10 messages from different people, including senderId 25
        val customerIds = listOf(1, 2, 3, 4, 5, 25, 7, 8, 9, 10) // Including senderId 25
        val messagesJsonArray = customerIds.map { customerId ->
            """
        {
            "id": ${System.currentTimeMillis()},
            "gistId": "someGistId",
            "senderId": $customerId,
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