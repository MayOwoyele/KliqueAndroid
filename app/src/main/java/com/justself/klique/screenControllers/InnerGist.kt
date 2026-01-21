package com.justself.klique.screenControllers

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import com.justself.klique.BuildConfig
import com.justself.klique.R
import com.justself.klique.SessionManager
import com.justself.klique.UserDetails
import com.justself.klique.databinding.InnerGistPageBinding
import com.justself.klique.gists.adapter.GistCarouselManager
import com.justself.klique.gists.adapter.InnerCarouselAdapter
import com.justself.klique.gists.adapter.TimelineItem
import com.justself.klique.gists.data.models.Post
import com.justself.klique.gists.data.models.PostType
import com.justself.klique.nav.KliqueVm
import com.justself.klique.nav.NavigationManager
import com.justself.klique.nav.ViewController
import com.justself.klique.nav.vm
import com.justself.klique.top_level_vm.CarouselViewModel
import com.justself.klique.top_level_vm.actualDownloadAndMap
import com.justself.klique.useful_objects.ProfileImageObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class InnerGist(
    override val nav: NavigationManager,
    val post: Post,
    override val context: Context
) : ViewController(), GistCarouselScreen, ResumableView {
    override lateinit var manager: GistCarouselManager
    override fun onResumeView() {
        manager.triggerSnapCheck()
    }

    val vm = nav.vm{ InnerGistViewModel(this, post) }
    override fun returnView(): View {
        val binding =
            InnerGistPageBinding.inflate(LayoutInflater.from(context), nav.theContainer, false)
        screenScope.launch {
            UserDetails.userDetails.collect { details ->
                ProfileImageObject.getOrDownloadThumbnail(
                    SessionManager.customerId.value,
                    details?.profileUrl ?: "",
                    scope = screenScope
                ) {
                    binding.bottomEnterPost.carouselPp.setImageURI(Uri.fromFile(it))
                }
            }
        }
        val paddingApplication = { topPadding: Int, bottomPadding: Int ->
            binding.topBar.setPadding(
                binding.topBar.paddingLeft,
                topPadding,
                binding.topBar.paddingRight,
                binding.topBar.paddingBottom
            )
            binding.bottomEnterPost.root.setPadding(
                binding.bottomEnterPost.root.paddingLeft,
                binding.bottomEnterPost.root.paddingTop,
                binding.bottomEnterPost.root.paddingRight,
                bottomPadding
            )
        }

        callPaddingMethod(binding.topBar, binding.bottomEnterPost.root, paddingApplication)
        val topScroll = { atTop: Boolean, scrollToTop: () -> Unit ->
            if (atTop){
                binding.upArrow.visibility = View.GONE
            } else {
                with(binding.upArrow){
                    visibility = View.VISIBLE
                    setOnClickListener{
                        scrollToTop()
                    }
                }
            }
        }
        manager = GistCarouselManager(
            recyclerView = binding.innerGistRecyclerView,
            downloadMap = vm.downloadedGistsMedia,
            scope = screenScope,
            context = context,
            onViewTimeRecorded = { id, seconds -> vm.onPostViewTimeRecorded(id, seconds) },
            onSnappedStable = { item ->
                vm.handleSnappedPosStayed(item)
            },
            onSnapAtAll = {item ->
                if (item is TimelineItem.GistPost){
                    binding.bottomEnterPost.carouselTextBox.text = context.getString(R.string.comment_on, item.post.name)
                }
            },
            onTopScroll = topScroll
        )
        val adapter = InnerCarouselAdapter(
            items = vm.timelineItems.value.toMutableList(),
            downloadMap = vm.downloadedGistsMedia,
            context = context,
            clickAction = manager.snapAwareClickHandler,
            scope = screenScope,
            nav = nav
        )
        manager.bindAdapter(adapter)
        screenScope.launch {
            vm.timelineItems.collect { newItems ->
                adapter.addItems(newItems)
                vm.downloadTheMedia(context)
            }
        }
        binding.gistRoomBackArrow.setOnClickListener {
            nav.goBack()
        }
        binding.innerGistRecyclerView.adapter = adapter
        return binding.root
    }

    override fun callPaddingMethod(
        topView: View,
        bottomView: View,
        paddingApp: (Int, Int) -> Unit
    ) {
        setTopAndBottomPadding(topView, bottomView, paddingApp)
    }
}

class InnerGistViewModel(screen: ViewController, post: Post) : KliqueVm(screen), CarouselViewModel {
    private val _downloadedGistsMedia = MutableStateFlow<Map<String, File>>(mutableMapOf())
    override val downloadedGistsMedia = _downloadedGistsMedia.asStateFlow()
    private val _timelineItems =
        MutableStateFlow<List<TimelineItem>>(listOf(TimelineItem.GistPost(post)))
    override val timelineItems = _timelineItems.asStateFlow()
    override fun handleSnappedPosStayed(item: TimelineItem) {

    }

    override fun onPostViewTimeRecorded(thePostId: String, secondsViewed: Int) {
    }

    override fun loadGistsFromServer() {
        if (BuildConfig.DEBUG) {
            val tl = dummyPosts.map { TimelineItem.GistPost(it) }
            _timelineItems.value += tl
        }
    }

    override fun downloadTheMedia(context: Context) {
        vmScope.launch(Dispatchers.IO) {
            delay(5000)
            val tempGist = mutableListOf<TimelineItem>()
            for (item in _timelineItems.value) {
                when (item) {
                    is TimelineItem.GistPost -> {
                        if (item.post.postType == PostType.Audio || item.post.postType == PostType.Video || item.post.postType == PostType.Image) {
                            val file = _downloadedGistsMedia.value[item.post.postId]
                            if (file == null) {
                                tempGist.add(item)
                            }
                        }
                    }
                }
            }
            actualDownloadAndMap(tempGist, context = context, _downloadedGistsMedia)
        }
    }

    private val dummyPosts: List<Post> = List(20) { index ->
        val userId = if (index % 2 == 0) 2 else 3
        Post(
            postId = "post_$index",
            name = if (userId == 2) "Adaeze" else "Chuka",
            profileImage = "https://example.com/profile_$userId.jpg",
            isVerified = index % 4 == 0,
            userId = userId,
            text = "This is dummy post number $index by user $userId",
            views = (50..1000).random(),
            superViews = (0..100).random(),
            commentCount = (0..10000).random(),
            postType = when (index % 4) {
                0 -> PostType.Text
                1 -> PostType.Image
                2 -> PostType.Audio
                else -> PostType.Video
            },
            superViewed = index % 3 == 0,
            mediaLink = when (index % 4) {
                0 -> null
                1 -> "https://example.com/image_$index.jpg"
                2 -> "https://example.com/audio_$index.m4a"
                else -> "https://example.com/video_$index.mp4"
            },
            gistReply = null,
            isFollowing = index % 5 == 0
        )
    }

    init {
        loadGistsFromServer()
    }
}