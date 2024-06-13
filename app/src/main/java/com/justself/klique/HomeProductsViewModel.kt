package com.justself.klique

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException


class ProductViewModel : ViewModel() {
    private val _products = MutableLiveData<Resource<List<Product>>>()
    val products: LiveData<Resource<List<Product>>> = _products

    private var currentPage = 1
    private val allProducts = mutableListOf<Product>()
    private val _currentMarketId = MutableLiveData<Int?>(1)
    private val _cartItems = MutableLiveData<List<Product>>(emptyList())
    val cartItems: LiveData<List<Product>> = _cartItems
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    private val _marketProducts = MutableLiveData<Resource<List<Product>>>()
    val marketProducts: LiveData<Resource<List<Product>>> = _marketProducts
    // Use map directly on LiveData
    val cartItemCount: LiveData<Int> = _cartItems.map { cartItems ->
        cartItems.sumOf { it.quantity }
    }

    private val _generalMarketId = MutableLiveData<Int?>(1)
    private val _marketSpecificId = MutableLiveData<Int?>()
    // Separate lists for different contexts
    private val generalProducts = mutableListOf<Product>()
    private val marketSpecificProducts = mutableListOf<Product>()
    private var generalCurrentPage = 1
    private var marketSpecificCurrentPage = 1


    init {
        fetchProducts() // Correctly accessing a private method from within the class

    }
    fun addToCart(productToAdd: Product) {
        val currentItems = _cartItems.value ?: emptyList()

        // Check if adding products from the same market or if the cart is initially empty
        if (currentItems.isNotEmpty() && _currentMarketId.value != productToAdd.marketId) {
            _errorMessage.value = "Only products from the same market can be added. Clear cart to add products from a different market."
            return
        }

        val existingProductIndex = currentItems.indexOfFirst { it.productId == productToAdd.productId }
        if (existingProductIndex >= 0) {
            val updatedList = currentItems.toMutableList()
            val existingProduct = updatedList[existingProductIndex]
            updatedList[existingProductIndex] = existingProduct.copy(quantity = existingProduct.quantity + 1)
            _cartItems.value = updatedList
        } else {
            _cartItems.value = currentItems + productToAdd.copy(quantity = 1)
            _currentMarketId.value = productToAdd.marketId  // Update market ID when adding to empty cart
        }
        _errorMessage.value = null
    }
    fun removeFromCart(productId: Int) {
        val currentItems = _cartItems.value ?: emptyList()
        val updatedList = currentItems.toMutableList()
        val itemIndex = updatedList.indexOfFirst { it.productId == productId }

        if (itemIndex != -1) {
            val product = updatedList[itemIndex]
            if (product.quantity > 1) {
                updatedList[itemIndex] = product.copy(quantity = product.quantity - 1)
            } else {
                updatedList.removeAt(itemIndex)
                // Reset market ID if cart is empty after removal
                if (updatedList.isEmpty()) {
                    _currentMarketId.value = null
                }
            }
            _cartItems.value = updatedList
        }
    }


    fun setGeneralMarketId(marketId: Int) {
        _generalMarketId.value = marketId
        fetchProducts(marketId = marketId)  // Fetch general products for new market ID
    }

    fun setMarketSpecificId(marketId: Int) {
        _marketSpecificId.value = marketId
        fetchMarketProducts(marketId = marketId)  // Fetch market-specific products
    }
    fun fetchMarketProducts(page: Int = marketSpecificCurrentPage, marketId: Int = _marketSpecificId.value ?: 1) = viewModelScope.launch {
        // Post the loading state only if it's the first page.
        if (page == 1) {
            _marketProducts.postValue(Resource.Loading())
        }

        val params = mapOf("action" to "homeScreenProducts", "page" to page.toString(), "market_id" to marketId.toString())
        try {
            val response = NetworkUtils.makeRequest("api.php", "GET", params)
            val newProducts = JsonConfig.json.decodeFromString<List<Product>>(response)

            if (page == 1) {
                marketSpecificProducts.clear()
            }
            marketSpecificProducts.addAll(newProducts)

            // Always post the success state to update the UI with either the new full list or appended data.
            _marketProducts.postValue(Resource.Success(ArrayList(marketSpecificProducts)))  // Post a copy to ensure LiveData notices the change
        } catch (e: IOException) {
            _marketProducts.postValue(Resource.Error("Network request failed: ${e.message}", 400))
        } catch (e: Exception) {
            _marketProducts.postValue(Resource.Error("An unexpected error occurred: ${e.message}", 500))
        }
    }

    fun loadMoreMarketProducts(marketId: Int) {
        marketSpecificCurrentPage++
        fetchMarketProducts(marketSpecificCurrentPage, marketId)
    }
    fun fetchProducts(page: Int = generalCurrentPage, marketId: Int = _generalMarketId.value ?: 1) = viewModelScope.launch {
        // Only post loading state if it's the first page
        if (page == 1) {
            _products.postValue(Resource.Loading())
        }

        val params = mapOf("action" to "homeScreenProducts", "page" to page.toString(), "market_id" to marketId.toString())
        try {
            val response = NetworkUtils.makeRequest("api.php", "GET", params)
            val newProducts = JsonConfig.json.decodeFromString<List<Product>>(response)

            with(generalProducts) {
                if (page == 1) clear()  // Clear only if it's the first page
                addAll(newProducts)  // Append new products to the existing list
            }

            // Post the same list instance, now containing either new or additional products
            _products.value = Resource.Success(ArrayList(generalProducts)) // Use ArrayList to force LiveData to recognize a change
        } catch (e: IOException) {
            _products.postValue(Resource.Error("Network request failed: ${e.message}", 400))
        } catch (e: Exception) {
            _products.postValue(Resource.Error("An unexpected error occurred: ${e.message}", 500))
        }
    }


    fun loadMoreProducts(marketId: Int) {
        generalCurrentPage++
        fetchProducts(generalCurrentPage, marketId)
    }
    fun likeProduct(productId: Int, customerId: Int) {
        viewModelScope.launch {
            try {
                val params = mapOf(
                    "action" to "addLike",
                    "productId" to productId.toString(),
                    "customerId" to customerId.toString()
                )
                val responseString = NetworkUtils.makeRequest(
                    endpoint = "api.php",
                    method = "POST",
                    params = params
                )
                Log.i("ProductViewModel", "Response received for like request: $responseString")

                // Parse the JSON response
                val response = Json.decodeFromString<LikeResponse>(responseString)

                // Check if the response was successful and contains a new likes count
                if (response.success && response.likesCount != null) {
                    val updatedProducts = allProducts.map { product ->
                        if (product.productId == productId) product.copy(likes = response.likesCount) else product
                    }
                    _products.postValue(Resource.Success(updatedProducts))
                    Log.d("ProductViewModel", "Updated products list successfully posted with new likes count: ${response.likesCount}.")
                } else {
                    // Handle possible errors
                    _errorMessage.postValue(response.error ?: "An unknown error occurred")
                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "An error occurred during the like request: ${e.message}", e)
                _errorMessage.postValue("An error occurred: ${e.message}")
            }
        }
    }
}
