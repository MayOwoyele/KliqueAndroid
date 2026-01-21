package com.justself.klique.screenControllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.justself.klique.databinding.BioViewBinding
import com.justself.klique.nav.NavigationManager
import com.justself.klique.nav.TabChild

class BookShelfScreenController(
    override val container: FrameLayout,
    override val nav: NavigationManager,
    override val vm: BookShelfScreenVm
) : TabChild<BookShelfScreenVm> {
    override fun createContentView(context: Context): View {
        val binding = BioViewBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }
}