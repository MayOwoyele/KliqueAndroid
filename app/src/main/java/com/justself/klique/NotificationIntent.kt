package com.justself.klique
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationIntentManager {
    private var _navigationRoute = MutableStateFlow<String?>(null)
    val navigationRoute = _navigationRoute.asStateFlow()
    fun updateNavigationRoute(route: String) {
        Logger.d("NotificationIntentManager", "Setting navigation route: $route")
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