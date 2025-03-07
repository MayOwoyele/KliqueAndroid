// File: WebSocketManager.kt
package com.justself.klique

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val listeners = mutableMapOf<String, WebSocketListener<*>>()
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    private var reconnectionAttempts: Int = 0
        set(value) {
            println("Reconnection attempts updated: $field -> $value")
            field = value
        }
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
    var aReconnection = false
    private var isPingPongRunning = false
    private var isConnecting = false
    private var reconnectionJob: Job? = null
    var isGistFormVisible = false
    private var websocketBuffer = mutableListOf<String>()

    @Volatile
    private var isReconnecting = false


    fun <T> registerListener(listener: WebSocketListener<T>) {
        listeners[listener.listenerId] = listener
    }

    fun <T> unregisterListener(listener: WebSocketListener<T>) {
        listeners.remove(listener.listenerId)
    }

    private fun createWebSocketClient(url: URI, context: Context): WebSocketClient {
        return object : WebSocketClient(url) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connected!")
                _isConnected.value = true
                shouldReconnect = true
                lastPongTime = System.currentTimeMillis()
                reconnectionAttempts = 0
                startPingPong()
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)
                    chatScreenViewModel?.retryPendingMessages(context)
                    chatScreenViewModel?.currentChat?.value?.let { enemyId ->
                        chatScreenViewModel?.subscribeToOnlineStatus(enemyId)
                    }
                    chatScreenViewModel?.fetchNewMessagesFromServer()
                    CoroutineScope(Dispatchers.IO).launch {
                        val randomNumber = (1..100).random()
                        if (randomNumber in 1..10) {
                            val theIds = chatScreenViewModel?.fetchRelevantIds()
                            if (theIds != null) {
                                chatScreenViewModel?.sendJsonToUpdateProfile(theIds)
                            }
                        }
                    }
                }
                sharedCliqueViewModel?.gistCreatedOrJoined?.value?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        sharedCliqueViewModel?.restoreGistState()
                    }
                    aReconnection = false
                }
                chatRoomViewModel?.retrial()
                sendTheBuffer()
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
                        if (bytes.remaining() < 12) {
                            println("Error: ByteBuffer does not have enough bytes for the prefix")
                            return@launch
                        }

                        val prefixBytes = ByteArray(12)
                        bytes.get(prefixBytes)
                        val prefix = String(prefixBytes, Charsets.UTF_8).trim()

                        if (bytes.remaining() <= 0) {
                            println("Error: No binary data found after prefix")
                            return@launch
                        }

                        val binaryData = ByteArray(bytes.remaining())
                        bytes.get(binaryData)

                        val jsonObject = JSONObject().apply {
                            put("prefix", prefix)
                            put("binaryData", Base64.encodeToString(binaryData, Base64.DEFAULT))
                        }

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
                println("WebSocket closed: Code - $code, Reason - $reason, Remote - $remote")

                when (code) {
                    1000 -> {
                        setIsConnectedToFalse("onClose: 1000")
                        println("WebSocket closed normally.")
                    }

                    4401 -> {
                        println("WebSocket closed due to expired token: $reason")
                        if (_isConnected.value) {
                            setIsConnectedToFalse("onClose: 4401")
                        }
                        handleUnauthorized()
                    }

                    4403 -> {
                        println("WebSocket closed due to invalid token: $reason")
                        handleForbidden()
                    }

                    else -> {
                        setIsConnectedToFalse("onClose: else")
                        println("WebSocket closed with unexpected code: $code, Reason: $reason")
                    }
                }
            }

            override fun onError(ex: Exception) {
                println("WebSocket error: ${ex.message}")
                if (shouldReconnect) {
                    CoroutineScope(Dispatchers.IO).launch {
                        setIsConnectedToFalse("onError")
                    }
                }
            }
        }.apply {
            if ("wss".equals(url.scheme, ignoreCase = true)) {
                setSocketFactory(sslContext.socketFactory)
            }
        }
    }

    fun connect(customerId: Int, fullName: String, context: Context, caller: String) {
        shouldReconnect = true
        if (isConnecting) {
            return
        }
        val url = context.getString(R.string.websocket_url)
        isConnecting = true
        val encodedFullName = URLEncoder.encode(fullName, "UTF-8")
        val jwtToken =
            JWTNetworkCaller.fetchAccessToken()?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        val uri = URI.create(url)
            .resolve("?customer_id=$customerId&full_name=$encodedFullName&token=$jwtToken")
        this.customerId = customerId
        this.fullName = fullName

        Log.d("RawWebsocket", "Caller $caller, Connecting to URI: $uri")
        webSocketClient = createWebSocketClient(uri, context).apply {
            connect()
        }
        isConnecting = false
    }

    fun setChatScreenViewModel(viewModel: ChatScreenViewModel) {
        chatScreenViewModel = viewModel
    }

    fun setSharedCliqueViewModel(viewModel: SharedCliqueViewModel) {
        sharedCliqueViewModel = viewModel
    }

    fun setChatRoomViewModel(viewModel: ChatRoomViewModel) {
        chatRoomViewModel = viewModel
    }

    fun clearChatRoomViewModel() {
        chatRoomViewModel = null
    }

    private suspend fun scheduleReconnect(caller: String) {
        Log.d("Reconnection", "Called")
        if (!shouldReconnect) {
            println("Reconnection aborted: shouldReconnect is false.")
            return
        }
        if (isReconnecting) {
            return
        }
        isReconnecting = true
        println("Reconnection attempt called again. Caller: $caller")
        aReconnection = true
        try {
            reconnectionJob = coroutineScope.launch {
                if (reconnectionAttempts < MAX_RECONNECT_ATTEMPTS && shouldReconnect) {
                    reconnectionAttempts++
                    println("Attempting to reconnect... (Attempt $reconnectionAttempts)")
                    delay(RECONNECT_DELAY * reconnectionAttempts)
                    if (!_isConnected.value) {
                        connect(
                            customerId, fullName,
                            appContext, "recon"
                        )
                    }
                } else {
                    println("Max reconnection attempts reached. Maintaining steady reconnect interval.")
                    while (!_isConnected.value && shouldReconnect) {
                        delay(RECONNECT_DELAY * reconnectionAttempts)
                        println("Attempting to reconnect at a steady interval...")
                        connect(
                            customerId, fullName,
                            appContext, "recon 2"
                        )
                    }
                }
            }
            reconnectionJob?.join()
        } finally {
            println("This attempt function bottomed out")
            isReconnecting = false
        }
    }

    private fun startPingPong() {
        Log.d("Websocket", "Calling send Ping")
        if (isPingPongRunning) return
        isPingPongRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            while (_isConnected.value) {
                try {
                    Log.d("Websocket", "Sending Ping")
                    Log.d("Websocket", "${webSocketClient != null}")
                    webSocketClient?.sendPing()
                    Log.d("Websocket", "First Sending Ping after delay")
                    delay(PING_INTERVAL)
                    Log.d("Websocket", "Sending Ping after delay")

                    if (System.currentTimeMillis() - lastPongTime > PONG_TIMEOUT) {
                        println("Pong timeout. Reconnecting...")
                        setIsConnectedToFalse("PingPong")
                    }
                } catch (e: Exception) {
                    println("Websocket Error during ping-pong: ${e.message}")
                    setIsConnectedToFalse("PingPong exception")
                }
            }
            isPingPongRunning = false
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSettingIsConnected: Boolean = false
    private fun setIsConnectedToFalse(caller: String) {
        Log.d("LifecycleOwner", caller)
        coroutineScope.launch {
            if (isSettingIsConnected) {
                return@launch
            }
            isSettingIsConnected = true
            webSocketClient?.close()
            _isConnected.value = false
            reconnectionAttempts = 0
            shouldReconnect = true
            scheduleReconnect("setIsConnectedToFalse")
            isSettingIsConnected = false
        }
    }

    fun send(message: String, showToast: Boolean = false, saveToBuffer: Boolean = false) {
        Log.d("RawWebsocket", "outgoing $message")
        if (!_isConnected.value && showToast) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    appContext,
                    "You should try again when wifi icon changes back to pink",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        try {
            if (webSocketClient?.isOpen == true) {
                webSocketClient?.send(message)
            } else {
                if (saveToBuffer) {
                    saveMessageToBuffer(message)
                }
                Log.e("WebSocketManager", "WebSocket is not connected. Message queued.")
            }
        } catch (e: WebsocketNotConnectedException) {
            Log.e("WebSocketManager", "WebSocket not connected: ${e.message}")
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
        } catch (e: Exception) {
            Log.e("WebSocketManager", "Exception in sending binary data: ${e.message}")
        }
    }

    fun close() {
        shouldReconnect = false
        isPingPongRunning = false
        reconnectionJob?.cancel()

        webSocketClient?.let {
            if (it.isOpen) {
                it.close(1000, "App closed WebSocket")
                Log.d("WebSocketManager", "WebSocket is being closed.")
            } else {
                Log.d("WebSocketManager", "WebSocket was already closed or not open.")
            }
        }
        _isConnected.value = false
        Log.d("LifecycleEvent", "Called WebSocket close function.")
    }

    private fun routeBinaryMessageToViewModel(prefix: String, jsonObject: JSONObject) {
    }

    private fun routeMessageToViewModel(type: String, targetId: String, jsonObject: JSONObject) {
        Log.d("Websocket", "Type is :$type")
        val listenerId = when {
            SharedCliqueReceivingType.entries.any { it.type == type } -> ListenerIdEnum.SHARED_CLIQUE.theId
            PrivateChatReceivingType.entries.any { it.type == type } -> ListenerIdEnum.PRIVATE_CHAT_SCREEN.theId
            ChatRoomReceivingType.entries.any { it.type == type } -> ListenerIdEnum.CHAT_ROOM_VIEW_MODEL.theId
            DmReceivingType.entries.any { it.type == type } -> ListenerIdEnum.DM_ROOM_VIEW_MODEL.theId
            else -> targetId
        }

        listenerId.let { id ->
            when (id) {
                ListenerIdEnum.SHARED_CLIQUE.theId -> {
                    val enumType = SharedCliqueReceivingType.entries.find { it.type == type }
                    @Suppress("UNCHECKED_CAST")
                    enumType?.let {
                        (listeners[id] as? WebSocketListener<SharedCliqueReceivingType>)?.onMessageReceived(
                            it,
                            jsonObject
                        )
                    } ?: println("Unknown message type: $type for SharedCliqueViewModel")
                }

                ListenerIdEnum.PRIVATE_CHAT_SCREEN.theId -> {
                    val enumType = PrivateChatReceivingType.entries.find { it.type == type }
                    @Suppress("UNCHECKED_CAST")
                    enumType?.let {
                        (listeners[id] as? WebSocketListener<PrivateChatReceivingType>)?.onMessageReceived(
                            it,
                            jsonObject
                        )
                    } ?: println("Unknown message type: $type for ChatScreenViewModel")
                }

                ListenerIdEnum.CHAT_ROOM_VIEW_MODEL.theId -> {
                    val enumType = ChatRoomReceivingType.entries.find { it.type == type }
                    @Suppress("UNCHECKED_CAST")
                    enumType?.let {
                        (listeners[id] as? WebSocketListener<ChatRoomReceivingType>)?.onMessageReceived(
                            it,
                            jsonObject
                        )
                    } ?: println("Unknown message type: $type for ChatRoomViewModel")
                }

                ListenerIdEnum.DM_ROOM_VIEW_MODEL.theId -> {
                    val enumType = DmReceivingType.entries.find { it.type == type }
                    @Suppress("UNCHECKED_CAST")
                    enumType?.let {
                        (listeners[id] as? WebSocketListener<DmReceivingType>)?.onMessageReceived(
                            it,
                            jsonObject
                        )
                    }
                }

                else -> {
                    println("Unhandled message type or target ID: $type, $targetId")
                }
            }
        }
    }
    private fun sendTheBuffer(){
        if (websocketBuffer.isNotEmpty() && isGistFormVisible){
            for (message in websocketBuffer) {
                send(message)
            }
        }
        websocketBuffer.clear()
    }
    private fun saveMessageToBuffer(message: String){
        websocketBuffer.add(message)
    }

    private fun handleUnauthorized() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("refreshToken", "Websocket, token")
            val responseCode = JWTNetworkCaller.refreshAccessToken()
            Log.d("StatusCode", responseCode.toString())
            if (responseCode == 200) {
                connect(
                    SessionManager.customerId.value,
                    SessionManager.fullName.value,
                    appContext, "unauthorized"
                )
                Log.d("refreshToken", "Websocket, token")
            } else if (responseCode == 403) {
                Log.d("StatusCode", "printed")
                SessionManager.resetCustomerData()
            }
        }
    }

    private fun handleForbidden() {
        CoroutineScope(Dispatchers.IO).launch {
            SessionManager.resetCustomerData()
            shouldReconnect = false
        }
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