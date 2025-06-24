package com.justself.klique.screenControllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.justself.klique.databinding.BottomAndTopBarBinding
import com.justself.klique.nav.KliqueVMStore
import com.justself.klique.nav.ScreenController

class TabbedBaseController(
    private val context: Context,
    private val viewModelStore: KliqueVMStore
) : ScreenController {

    private lateinit var rootView: View
    private var currentTab: String = "home"

    override fun returnView(): View {
        if (::rootView.isInitialized) return rootView

        val binding = BottomAndTopBarBinding.inflate(LayoutInflater.from(context))
        rootView = binding.root

        val homeContainer      = binding.homeContainer
        val chatsContainer     = binding.chatsContainer
        val bookshelfContainer = binding.bookshelfContainer

        val tabMap = mapOf(
            "home" to homeContainer,
            "chats" to chatsContainer,
            "bookshelf" to bookshelfContainer
        )

        fun switchToTab(tab: String) {
            currentTab = tab
            tabMap.forEach { (key, frame) ->
                frame.visibility = if (key == tab) View.VISIBLE else View.GONE
            }

            if (tabMap[tab]?.childCount == 0) {
                val screenController = when (tab) {
                    "home" -> HomeScreenController(context, viewModelStore, homeContainer)
                    "chats" -> ChatsScreenController(context, viewModelStore)
                    "bookshelf" -> BookShelfScreenController(context, viewModelStore)
                    else -> error("Unknown tab: $tab")
                }
                tabMap[tab]?.addView(screenController.returnView())
            }
        }

        binding.homeView.setOnClickListener {
            switchToTab("home")
        }
        binding.chatsView.setOnClickListener {
            switchToTab("chats")
        }
        binding.bookShelf.setOnClickListener {
            switchToTab("bookshelf")
        }

        switchToTab(currentTab)
        return rootView
    }
}