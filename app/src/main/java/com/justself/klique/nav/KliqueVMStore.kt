package com.justself.klique.nav

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.reflect.KClass

/**
 * Represents a ViewModel that is scoped to a specific [ViewController].
 *
 * This interface ensures that every ViewModel is directly associated with a specific screen instance.
 * It enables lifecycle-aware screen-viewmodel relationships while still allowing flexibility in
 * how screens and view models are managed.
 *
 * @param S The type of [ViewController] that this ViewModel is scoped to.
 */
private interface ScreenViewModel {
    val screen: ViewController
    /**
     * Called when this ViewModel is removed from the [KliqueVMStore].
     *
     * You can override this to clean up resources, cancel coroutines, or perform teardown logic.
     */
    fun onCleared() {}
}
/**
 * A custom ViewModel store that maps [ScreenController] instances to their corresponding [ScreenViewModel]s.
 *
 * This is an alternative to Android's built-in ViewModel system, tailored for apps using manual
 * screen controller patterns rather than Fragments or Compose Navigation.
 *
 * ViewModels are created lazily and automatically cached for each unique screen instance.
 * When a screen is popped or destroyed, the ViewModel is removed and [ScreenViewModel.onCleared] is invoked.
 */
class KliqueVMStore {

    private val store = mutableMapOf<Pair<ViewController, KClass<out ScreenViewModel>>, ScreenViewModel>()

    @Suppress("UNCHECKED_CAST")
    fun <VM : ScreenViewModel> get(factory: () -> VM): VM {
        val temp = factory()
        val key = temp.screen to temp::class
        return (store[key] as? VM) ?: temp.also {
            store[key] = it
        }
    }

    fun clear(screen: ViewController) {
        val keysToRemove = store.keys.filter { it.first == screen }
        keysToRemove.forEach { key ->
            store.remove(key)?.onCleared()
        }
    }

    fun clearAll() {
        store.values.forEach { it.onCleared() }
        store.clear()
    }
}

abstract class KliqueVm(
    final override val screen: ViewController
) : ScreenViewModel {
    private val vmJob = Job()
    protected val vmScope = CoroutineScope(Dispatchers.Main.immediate + vmJob)
    override fun onCleared() = vmJob.cancel()
}

fun <VM : ScreenViewModel> NavigationManager.vm(
    factory: () -> VM
): VM {
    return viewModelStore.get(factory)
}