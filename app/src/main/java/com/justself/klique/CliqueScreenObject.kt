package com.justself.klique

import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.justself.klique.MyKliqueApp.Companion.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONObject

val Context.cliqueDataStore by preferencesDataStore("clique_prefs")

object CliqueScreenObject : WebSocketListener<CliqueRequestReceivingType> {
    val list = if (BuildConfig.DEBUG) {
        listOf(
            Clique(userId = 3, name = "May", isVerified = true, profileImage = ""),
            Clique(userId = 5, name = "Owoyele", isVerified = false, profileImage = "")
        )
    } else {
        emptyList()
    }

    private const val PREFS_NAME = "klique_prefs"
    private const val KEY_UNREAD = "clique_unread_count"

    // Acquire prefs once
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _uncheckedListCount = MutableStateFlow(
        prefs.getInt(KEY_UNREAD, 0)
    )
    val uncheckedListCount = _uncheckedListCount.asStateFlow()
    private val _cliqueMembersRequest = MutableStateFlow<List<Clique>>(list)
    val cliqueMembersRequest = _cliqueMembersRequest.asStateFlow()
    private val _myCliqueMembers = MutableStateFlow<List<Clique>?>(null)
    val myCliqueMembers = _myCliqueMembers.asStateFlow()

    fun initialize() {
        registerListener()
        loadFromDataStore()
        fetchCliqueRequests()
    }

    private val CLIQUE_KEY = stringPreferencesKey("cached_clique")
    private suspend fun Context.saveCliqueList(clique: List<Clique>) {
        val json = Json.encodeToString(ListSerializer(Clique.serializer()), clique)
        cliqueDataStore.edit { prefs ->
            prefs[CLIQUE_KEY] = json
        }
    }

    private fun Context.observeCliqueList(): Flow<List<Clique>> =
        cliqueDataStore.data
            .map { prefs ->
                prefs[CLIQUE_KEY]
                    ?.let { Json.decodeFromString(ListSerializer(Clique.serializer()), it) }
                    ?: emptyList()
            }

    override val listenerId = ListenerIdEnum.CLIQUE_SCREEN.theId
    override fun onMessageReceived(type: CliqueRequestReceivingType, jsonObject: JSONObject) {
        when (type) {
            CliqueRequestReceivingType.CLIQUE_REQUESTS -> {
                Logger.d("CliqueJoin", "CliqueRequests: $jsonObject")
                val cliqueList = mutableListOf<Clique>()
                val cliqueRequests = jsonObject.getJSONArray("cliqueRequests")
                for (theIndex in 0 until cliqueRequests.length()) {
                    val name = cliqueRequests.getJSONObject(theIndex).getString("name")
                    val userId = cliqueRequests.getJSONObject(theIndex).getInt("userId")
                    val profileUrl =
                        cliqueRequests.getJSONObject(theIndex).getString("profilePicture")
                    val isVerified = cliqueRequests.getJSONObject(theIndex).getBoolean("isVerified")
                    val clique = Clique(userId, name, isVerified, profileUrl)
                    cliqueList.add(clique)
                }
                MediaVM.scope.launch {
                    updateCliqueRequests(CliqueRequestsUpdateType.Add, users = cliqueList)
                }
            }

            CliqueRequestReceivingType.CLIQUE_DECLINE_REQUEST -> {
                val userId = jsonObject.getInt("userId")
                MediaVM.scope.launch {
                    updateCliqueRequests(
                        CliqueRequestsUpdateType.Subtract,
                        users = MutableList(1) {
                            Clique(
                                userId = userId,
                                name = "",
                                isVerified = false,
                                profileImage = ""
                            )
                        })
                }
            }
        }
    }

    private fun registerListener() {
        WebSocketManager.registerListener(this)
    }

    fun fetchCliqueRequests() {
        val data = JSONObject().put("type", "fetchCliqueRequests")
        val theItem = BufferObject(
            type = WsDataType.CliqueRequests,
            message = data.toString()
        )
        send(theItem)
    }

    fun send(sendItem: BufferObject) {
        WebSocketManager.send(sendItem)
    }

    fun sendCliqueRequest(userId: Int) {
        val json = JSONObject().put("userId", userId)
        val action: suspend (NetworkUtils.JwtTriple) -> Unit = {
            withContext(Dispatchers.Main){
                Toast.makeText(appContext, "Clique request sent successfully", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {
            withContext(Dispatchers.Main){
                Toast.makeText(
                    appContext,
                    "Error sending clique request, please try again later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        MediaVM.scope.launch(Dispatchers.IO) {
            NetworkUtils.makeJwtRequest(
                "sendJoinCliqueRequest",
                KliqueHttpMethod.POST,
                emptyMap(),
                json.toString(),
                action = action,
                errorAction = errorAction
            )
        }
    }

    fun acceptRequest(user: Clique) {
        val action: suspend (NetworkUtils.JwtTriple) -> Unit = {
            val mutableList = MutableList(1) { user }
            updateCliqueRequests(CliqueRequestsUpdateType.Subtract, users = mutableList)
        }
        val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {
            withContext(Dispatchers.Main){
                Toast.makeText(
                    appContext,
                    "Error accepting request, please try again later",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val json = JSONObject().put("theUserId", user.userId)
        MediaVM.scope.launch(Dispatchers.IO) {
            NetworkUtils.makeJwtRequest(
                "acceptJoinRequest",
                KliqueHttpMethod.POST,
                emptyMap(),
                json.toString(),
                action = action,
                errorAction = errorAction
            )
        }
    }

    fun rejectRequest(userId: Int) {
        val json = JSONObject().apply {
            put("type", "rejectCliqueRequest")
            put("userId", userId)
        }
        val bufferObject = BufferObject(
            type = WsDataType.CliqueRequests,
            message = json.toString()
        )
        send(bufferObject)
    }

    fun fetchMyCliqueMembers() {
        fetchCliqueMembers(
            SessionManager.customerId.value,
            { _myCliqueMembers.value = it },
            MediaVM.scope
        )
    }

    private suspend fun updateCliqueRequests(
        requestType: CliqueRequestsUpdateType,
        users: MutableList<Clique>
    ) {
        when (requestType) {
            CliqueRequestsUpdateType.Add -> {
                val oldIds = _cliqueMembersRequest.value.map { it.userId }.toSet()
                val newCount = users.count { it.userId !in oldIds }
                incrementUnread(newCount)
                finalizeProcessing(users)
            }
            CliqueRequestsUpdateType.Subtract -> {
                val filtered = _cliqueMembersRequest.value
                    .filter { old -> old.userId !in users.map { it.userId } }
                finalizeProcessing(filtered)
            }
        }
    }
    private fun incrementUnread(by: Int = 1) {
        val updated = _uncheckedListCount.value + by
        prefs.edit().putInt(KEY_UNREAD, updated).apply()
        _uncheckedListCount.value = updated
    }

    private suspend fun finalizeProcessing(users: List<Clique>) {
        appContext.saveCliqueList(users)
    }

    private fun loadFromDataStore() {
        appContext.observeCliqueList()
            .onEach { _cliqueMembersRequest.value = it }
            .launchIn(MediaVM.scope)
    }

    fun resetUncheckedCount() {
        prefs.edit().putInt(KEY_UNREAD, 0).apply()
        _uncheckedListCount.value = 0
    }
}

enum class CliqueRequestsUpdateType {
    Add,
    Subtract
}