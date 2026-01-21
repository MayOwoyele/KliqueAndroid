package com.justself.klique.nav

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.justself.klique.Logger
import androidx.core.view.isEmpty
import com.justself.klique.AudioPlayer
import com.justself.klique.gists.adapter.AudioPlayerPool
import com.justself.klique.gists.adapter.ExoPlayerPool
import com.justself.klique.screenControllers.GistCarouselScreen
import com.justself.klique.screenControllers.ResumableView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NavigationManager(val theContainer: FrameLayout, val viewModelStore: KliqueVMStore) {
    private val theBackStack = ArrayDeque<ScreenEntry>()
    fun goTo(controller: ViewController) {
        Logger.d("NavManager", "view is launching")
        val newView = controller.returnView()
        Logger.d("NavManager", "view has returned again")
        val screenEntryObject = ScreenEntry(
            controller = controller,
            view = newView
        )
        Logger.d("NavManager", "there's a view")
        theBackStack.addLast(screenEntryObject)
        theContainer.removeAllViews()
        theContainer.addView(newView)
        newView.post{
            ViewCompat.requestApplyInsets(newView)
        }
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
        (current.controller as? ViewController)?.onDestroy()
        val carouselScreen = previous.controller as? ResumableView
        Logger.d("FooterHeight", "Carousel Screen? ${carouselScreen == null}")
        carouselScreen?.onResumeView()
        return true
    }

    /**
     * Clear all screens and ViewModels (e.g., on logout).
     */
    fun clear() {
        theBackStack.forEach {
            viewModelStore.clear(it.controller)
            (it.controller as? ViewController)?.onDestroy()
        }
        theBackStack.clear()
        theContainer.removeAllViews()
    }
}
private interface ScreenController {
    fun returnView(): View
    val tag: String get() = this::class.java.simpleName
    val nav: NavigationManager
    val context: Context
    fun callPaddingMethod(topView: View, bottomView: View, paddingApp: (Int, Int) -> Unit)
}
data class ScreenEntry(
    val controller: ViewController,
    val view: View
)
interface TabChild<VM: KliqueVm> {
    val container: FrameLayout
    val nav: NavigationManager
    val vm: VM
    val screenScope: CoroutineScope
        get() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    fun createContentView(context: Context): View
    fun attachIfEmpty() {
        if (container.isEmpty()) {
            container.addView(createContentView(container.context))
        }
    }
}
private object PaddingInsetCache {
    var cachedTopInset: Int? = null
    var cachedBottomInset: Int? = null
}
abstract class ViewController : ScreenController {
    private val screenJob = SupervisorJob()
    protected val screenScope = CoroutineScope(Dispatchers.Main + screenJob)


    open fun onDestroy() {
        screenJob.cancel()
        ExoPlayerPool.releaseAll()
        AudioPlayerPool.releaseAll()
    }

    fun setTopAndBottomPadding(topView: View, bottomView: View, paddingApp: (Int, Int) -> Unit) {
        if (PaddingInsetCache.cachedTopInset != null && PaddingInsetCache.cachedBottomInset != null ){
            paddingApp(PaddingInsetCache.cachedTopInset!!, PaddingInsetCache.cachedBottomInset!!)
            Logger.d("ToopInset", "Logging?")
        }

        ViewCompat.setOnApplyWindowInsetsListener(topView) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            PaddingInsetCache.cachedTopInset = topInset
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomView) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            PaddingInsetCache.cachedBottomInset = bottomInset
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }
    }
}