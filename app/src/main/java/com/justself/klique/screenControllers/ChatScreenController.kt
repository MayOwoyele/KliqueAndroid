package com.justself.klique.screenControllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.justself.klique.databinding.ChatListBinding
import com.justself.klique.nav.NavigationManager
import com.justself.klique.nav.TabChild

class ChatsScreenController(
    override val nav: NavigationManager,
    override val container: FrameLayout,
    override val vm: ChatsScreenVm
) : TabChild<ChatsScreenVm> {
    override fun createContentView(context: Context): View {
        return ChatListBinding.inflate(LayoutInflater.from(context)).root
    }
}