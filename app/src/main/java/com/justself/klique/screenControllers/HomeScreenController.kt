package com.justself.klique.screenControllers

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.justself.klique.Logger
import com.justself.klique.SessionManager
import com.justself.klique.UserDetails
import com.justself.klique.databinding.GistTlRecyclerBinding
import com.justself.klique.gists.adapter.GistCarouselAdapter
import com.justself.klique.gists.adapter.GistCarouselManager
import com.justself.klique.gists.adapter.TLCarouselAdapter
import com.justself.klique.nav.NavigationManager
import com.justself.klique.nav.TabChild
import com.justself.klique.top_level_vm.CarouselViewModel
import com.justself.klique.top_level_vm.HomeScreenVm
import com.justself.klique.useful_objects.ProfileImageObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
interface GistCarouselScreen{
    val manager: GistCarouselManager
}
interface ResumableView{
    fun onResumeView()
}
class HomeScreenController(
    override val nav: NavigationManager,
    override val container: FrameLayout,
    override val vm: HomeScreenVm
) : TabChild<HomeScreenVm>, GistCarouselScreen {
    override lateinit var manager: GistCarouselManager
    override fun createContentView(context: Context): View {
        val binding = GistTlRecyclerBinding.inflate(LayoutInflater.from(context))
        manager = GistCarouselManager(
            recyclerView = binding.theRecyclerView,
            downloadMap = vm.downloadedGistsMedia,
            scope = screenScope,
            context = context,
            onViewTimeRecorded = { id, seconds -> vm.onPostViewTimeRecorded(id, seconds) },
            onSnappedStable = { item -> vm.handleSnappedPosStayed(item) },
        )
        screenScope.launch {
            UserDetails.userDetails.collect { details ->
                ProfileImageObject.getOrDownloadThumbnail(SessionManager.customerId.value, details?.profileUrl ?: "", scope = screenScope){
                    binding.startGistContainer.carouselPp.setImageURI(Uri.fromFile(it))
                }
            }
        }
        val animation: (ScrollDirection) -> Unit = { direction ->
            val target = binding.startGistContainer.startGistContainer
            val recycler = binding.theRecyclerView

            val targetLocation = IntArray(2)
            val recyclerLocation = IntArray(2)

            target.getLocationOnScreen(targetLocation)
            recycler.getLocationOnScreen(recyclerLocation)

            val targetTopY = targetLocation[1]
            val recyclerBottomY = recyclerLocation[1] + recycler.height

            val deltaY = recyclerBottomY - targetTopY

            when (direction) {
                ScrollDirection.ScrollUp -> {
                    target.animate()
                        .translationY(deltaY.toFloat())
                        .setDuration(250)
                        .start()
                }
                ScrollDirection.ScrollDown -> {
                    target.animate()
                        .translationY(0f)
                        .setDuration(250)
                        .start()
                }
            }
        }
        val adapter = TLCarouselAdapter(
            items = vm.timelineItems.value.toMutableList(),
            downloadMap = vm.downloadedGistsMedia,
            context = context,
            clickAction = manager.snapAwareClickHandler,
            scope = screenScope,
            nav = nav
        )
        manager.bindAdapter(adapter)
        manager.setOnScrollDirectionListener(animation)
        screenScope.launch {
            vm.timelineItems.collect { newItems ->
                adapter.addItems(newItems)
                vm.downloadTheMedia(context)
            }
        }
        binding.theRecyclerView.adapter = adapter
        screenScope.launch {
            vm.triggerReload.collect{
                if (it){
                    Logger.d("FooterHeight", "Snap checked")
                    manager.triggerSnapCheck()
                }
            }
        }
        binding.startGistContainer.startGistContainer.setOnClickListener{

        }
        vm.loadGistsFromServer()
        return binding.root
    }
}

enum class ScrollDirection{
    ScrollUp,
    ScrollDown
}