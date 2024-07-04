package com.justself.klique

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.IOException


class ShopViewModel : ViewModel() {
    private val _shopDetails = MutableLiveData<Resource<Shop>>()
    val shopDetails: LiveData<Resource<Shop>> = _shopDetails

    fun fetchShopDetails(shopId: Int, page: Int = 1) = viewModelScope.launch {
        _shopDetails.postValue(Resource.Loading())
        try {
            // Constructing the new endpoint URL
            val url = "fetchShopDetails/$shopId/$page"
            val response = NetworkUtils.makeRequest(url, "GET", emptyMap())
            val shop = JsonConfig.json.decodeFromString<Shop>(response)
            _shopDetails.postValue(Resource.Success(shop))
        } catch (e: IOException) {
            _shopDetails.postValue(Resource.Error("Failed to load shop details: ${e.message}", 500))
        } catch (e: Exception) {
            _shopDetails.postValue(Resource.Error("Failed to load shop details: ${e.message}", 500))
        }
    }
}