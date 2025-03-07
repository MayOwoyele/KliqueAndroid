package com.justself.klique
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationIntentManager {
    var currentIntent: Intent? = null
    private var _navigationRoute = MutableStateFlow<String?>(null)
    val navigationRoute = _navigationRoute.asStateFlow()
    fun updateNavigationRoute(route: String) {
        Log.d("NotificationIntentManager", "Setting navigation route: $route")
        _navigationRoute.value = route
    }
    private fun clearNavigationRoute() {
        _navigationRoute.value = null
    }
    fun executeNavigation(navigation: () -> Unit) {
        navigation()
        clearNavigationRoute()
    }
}