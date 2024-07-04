package com.justself.klique

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MarketViewModel : ViewModel() {
    private val _markets = mutableStateOf<List<Market>>(emptyList())
    val markets: State<List<Market>> = _markets

    init {
        fetchMarkets()
    }


    private val json = Json { ignoreUnknownKeys = true }

    private fun fetchMarkets() {
        viewModelScope.launch {
            val endpoint = "fetchMarkets"
            try {
                val result = NetworkUtils.makeRequest(endpoint, method = "GET", emptyMap())
                _markets.value = json.decodeFromString(result)
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Failed to fetch markets: ${e.message}")
            }
        }
    }
}
