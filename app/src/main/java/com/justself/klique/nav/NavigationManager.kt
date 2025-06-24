package com.justself.klique.nav

import android.view.View
import android.widget.FrameLayout

class NavigationManager(private val theContainer: FrameLayout, private val viewModelStore: KliqueVMStore) {
    private val theBackStack = ArrayDeque<ScreenEntry>()
    fun goTo(controller: ScreenController) {
        val newView = controller.returnView()
        val screenEntryObject = ScreenEntry(
            controller =controller,
            view = newView
        )
        theBackStack.addLast(screenEntryObject)
        theContainer.removeAllViews()
        theContainer.addView(newView)
    }

    /**
     * Pop the current screen; destroy its ViewModel, show previous.
     */
    fun goBack(): Boolean {
        if (theBackStack.size <= 1) return false

        val current = theBackStack.removeLast()
        viewModelStore.clear(current.controller)

        val previous = theBackStack.last()
        val view = previous.view
        theContainer.removeAllViews()
        theContainer.addView(view)
        return true
    }

    /**
     * Clear all screens and ViewModels (e.g., on logout).
     */
    fun clear() {
        theBackStack.forEach {
            viewModelStore.clear(it.controller)
        }
        theBackStack.clear()
        theContainer.removeAllViews()
    }
}
interface ScreenController {
    fun returnView(): View
    val tag: String get() = this::class.java.simpleName
}
data class ScreenEntry(
    val controller: ScreenController,
    val view: View
)