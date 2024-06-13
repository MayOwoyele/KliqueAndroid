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
            // Including 'action' and 'page' parameters along with 'shop_id'
            val params = mapOf(
                "action" to "fetchShopDetails", // This needs to match your PHP switch case
                "shop_id" to shopId.toString(),
                "page" to page.toString()  // Add page number to the parameters
            )
            // Using the base endpoint 'api.php' as all actions are routed through it
            val response = NetworkUtils.makeRequest("api.php", "GET", params)
            val shop = JsonConfig.json.decodeFromString<Shop>(response)
            _shopDetails.postValue(Resource.Success(shop))
        } catch (e: IOException) {
            _shopDetails.postValue(Resource.Error("Failed to load shop details: ${e.message}", 500))
        } catch (e: Exception) {
            _shopDetails.postValue(Resource.Error("Failed to load shop details: ${e.message}", 500))
        }
    }

}

