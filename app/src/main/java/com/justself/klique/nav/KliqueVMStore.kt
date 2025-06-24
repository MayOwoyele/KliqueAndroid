package com.justself.klique.nav
interface ScreenViewModel<S : ScreenController> {
    val screen: S
    fun onCleared() {}
}
class KliqueVMStore {
    private val store = mutableMapOf<ScreenController, ScreenViewModel<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <S : ScreenController, VM : ScreenViewModel<S>> get(
        screen: S,
        factory: (S) -> VM
    ): VM {
        return (store[screen] as? VM) ?: factory(screen).also {
            store[screen] = it
        }
    }

    fun clear(screen: ScreenController) {
        store.remove(screen)?.onCleared()
    }

    fun clearAll() {
        store.values.forEach { it.onCleared() }
        store.clear()
    }
}