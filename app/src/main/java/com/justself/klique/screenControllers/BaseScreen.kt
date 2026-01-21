package com.justself.klique.screenControllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.justself.klique.Logger
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.R
import com.justself.klique.databinding.BottomAndTopBarBinding
import com.justself.klique.nav.KliqueVm
import com.justself.klique.nav.NavigationManager
import com.justself.klique.nav.TabChild
import com.justself.klique.nav.ViewController
import com.justself.klique.nav.vm
import com.justself.klique.top_level_vm.HomeScreenVm

sealed class RootChildren<VM : KliqueVm>(
    val iconView: (BottomAndTopBarBinding) -> ImageView,
    val containerView: (BottomAndTopBarBinding) -> FrameLayout,
) {
    abstract fun createController(
        container: FrameLayout,
        nav: NavigationManager,
        vm: VM
    ): TabChild<VM>

    data object Gists : RootChildren<HomeScreenVm>(
        iconView = { it.gistsView },
        containerView = { it.homeContainer }
    ) {
        override fun createController(
            container: FrameLayout,
            nav: NavigationManager,
            vm: HomeScreenVm
        ): TabChild<HomeScreenVm> =
            HomeScreenController(nav, container, vm)
    }

    data object Chats : RootChildren<ChatsScreenVm>(
        iconView = { it.chatsView },
        containerView = { it.chatsContainer }
    ) {
        override fun createController(
            container: FrameLayout,
            nav: NavigationManager,
            vm: ChatsScreenVm
        ): TabChild<ChatsScreenVm> =
            ChatsScreenController(nav, container, vm)
    }

    data object BookShelf : RootChildren<BookShelfScreenVm>(
        iconView = { it.bookShelf },
        containerView = { it.bookshelfContainer }
    ) {
        override fun createController(
            container: FrameLayout,
            nav: NavigationManager,
            vm: BookShelfScreenVm
        ): TabChild<BookShelfScreenVm> =
            BookShelfScreenController(container, nav, vm)
    }

    companion object {
        val all: List<RootChildren<out KliqueVm>> by lazy {
            listOf(Gists, Chats, BookShelf)
        }
    }
}

class BaseScreen(
    override val context: Context,
    override val nav: NavigationManager
) : ViewController(), ResumableView {
    private lateinit var rootView: View
    private var currentTab: RootChildren<out KliqueVm> = RootChildren.Gists
    val homeVm = nav.vm { HomeScreenVm(this) }
    private val tabControllers =
        mutableMapOf<RootChildren<out KliqueVm>, TabChild<out KliqueVm>>()

    override fun returnView(): View {
        if (::rootView.isInitialized) return rootView
        val primaryColor = ContextCompat.getColor(appContext, R.color.primary)
        val grayColor = ContextCompat.getColor(appContext, R.color.gray)
        val binding =
            BottomAndTopBarBinding.inflate(LayoutInflater.from(context), nav.theContainer, false)
        val paddingApplication = { topPadding: Int, bottomPadding: Int ->
            binding.topBarIconsContainer.setPadding(
                binding.topBarIconsContainer.paddingLeft,
                topPadding,
                binding.topBarIconsContainer.paddingRight,
                binding.topBarIconsContainer.paddingBottom
            )
            binding.bottomNavBar.setPadding(
                binding.bottomNavBar.paddingLeft,
                binding.bottomNavBar.paddingTop,
                binding.bottomNavBar.paddingRight,
                bottomPadding
            )
        }
        callPaddingMethod(binding.topBarIconsContainer, binding.bottomNavBar, paddingApplication)
        rootView = binding.root
        Logger.d("BaseScreen", "Using RootChildren class from ${RootChildren::class.qualifiedName}")
        RootChildren.all.forEachIndexed { idx, item ->
            Logger.d("BaseScreen", "  all[$idx] = $item")
        }
        val tabMap = RootChildren.all.associateWith { it.containerView(binding) }

        fun switchToTab(tab: RootChildren<out KliqueVm>) {
            currentTab = tab

            // show/hide containers, color icons…
            tabMap.forEach { (key, frame) ->
                frame.visibility = if (key == tab) VISIBLE else GONE
            }

            RootChildren.all.forEach { child ->
                val icon = child.iconView(binding)
                icon.setColorFilter(if (child == tab) primaryColor else grayColor)
            }

            val controller = when (tab) {
                is RootChildren.Gists -> {
                    tab.createController(tabMap[tab]!!, nav, homeVm)
                }

                is RootChildren.Chats -> {
                    val vm = nav.vm { ChatsScreenVm(this) }
                    tab.createController(tabMap[tab]!!, nav, vm)
                }

                is RootChildren.BookShelf -> {
                    val vm = nav.vm { BookShelfScreenVm(this) }
                    tab.createController(tabMap[tab]!!, nav, vm)
                }
            }.also {
                tabControllers.getOrPut(tab) { it }
            }

            controller.attachIfEmpty()
        }
        RootChildren.all.forEach { tab ->
            val icon = tab.iconView(binding)
            icon.setOnClickListener { switchToTab(tab) }
        }
        switchToTab(currentTab)
        return rootView
    }

    override fun callPaddingMethod(topView: View, bottomView: View, paddingApp: (Int, Int) -> Unit) {
        setTopAndBottomPadding(topView, bottomView, paddingApp)
    }

    override fun onResumeView() {
        homeVm.setTrigger(true)
        homeVm.setTrigger(false)
    }
}

class ChatsScreenVm(screen: ViewController) : KliqueVm(screen)

class BookShelfScreenVm(screen: ViewController) : KliqueVm(screen)