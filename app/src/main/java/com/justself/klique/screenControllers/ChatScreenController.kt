package com.justself.klique.screenControllers

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.justself.klique.nav.KliqueVMStore
import com.justself.klique.nav.ScreenController
import androidx.core.view.isNotEmpty

class ChatsScreenController(
    private val context: Context,
    private val viewModelStore: KliqueVMStore,
    private val container: FrameLayout
) : ScreenController {

    override fun returnView(): View {
        if (container.isNotEmpty()) return container

//        val binding = ChatsContainerBinding.inflate(LayoutInflater.from(context), container, false)
//        container.addView(binding.root)
//
//        val viewModel = viewModelStore.get(this) { ChatsViewModel(this) }
//
//        binding.newChatButton.setOnClickListener {
//            viewModel.startNewChat()
//        }

        return container
    }
}