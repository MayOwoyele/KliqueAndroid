package com.justself.klique.gists.adapter

import android.content.Context
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.RecyclerView
import com.justself.klique.Logger
import com.justself.klique.customRecyclerView.ScaledCarousel
import com.justself.klique.customRecyclerView.TopSnapHelper
import com.justself.klique.gists.data.models.PostType
import com.justself.klique.screenControllers.ScrollDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class GistCarouselManager(
    private val recyclerView: RecyclerView,
    private val downloadMap: StateFlow<Map<String, File>>,
    private val scope: CoroutineScope,
    private val context: Context,
    private val onViewTimeRecorded: (String, Int) -> Unit,
    private val onSnappedStable: (TimelineItem) -> Unit,
    private val onSnapAtAll: ((TimelineItem) -> Unit)? = null,
    private val onTopScroll: ((Boolean, () -> Unit) -> Unit)? = null
) {
    private val snapHelper = TopSnapHelper()
    private var lastSnapped = RecyclerView.NO_POSITION
    private var lastSnappedPostId = ""
    private var viewStartTime: Long? = null
    private var lastSnappedViewHolder: PlayableViewHolder? = null
    private var snapActionJob: Job? = null
    private var lastMediaMap: Map<String, File> = emptyMap()
    lateinit var adapter: GistCarouselAdapter
        private set

    fun bindAdapter(adapter: GistCarouselAdapter) {
        this.adapter = adapter
        setupRecycler()
        setupFooterHeight()
        bindMediaUpdates()
        listenToSnapping()
        recyclerView.post {
            handleInitialSnap()
        }
    }

    private fun setupRecycler() {
        recyclerView.layoutManager = ScaledCarousel(context)
        snapHelper.attachToRecyclerView(recyclerView)
    }

    private fun handleInitialSnap() {
        val snappedView = recyclerView.layoutManager?.let { snapHelper.findSnapView(it) } ?: return
        val pos = recyclerView.getChildAdapterPosition(snappedView)
        if (pos == RecyclerView.NO_POSITION) return

        val holder = recyclerView.findViewHolderForAdapterPosition(pos) ?: return
        val playableViewHolder = holder as? PlayableViewHolder ?: return
        val item = adapter.items.getOrNull(holder.bindingAdapterPosition) ?: return

        lastSnappedViewHolder?.pauseMedia()
        lastSnappedViewHolder = playableViewHolder
        lastSnappedPostId = (item as? TimelineItem.GistPost)?.post?.postId ?: ""
        lastSnapped = pos
        viewStartTime = System.currentTimeMillis()

        if (item is TimelineItem.GistPost &&
            (item.post.postType == PostType.Video || item.post.postType == PostType.Audio)
        ) {
            downloadMap.value[item.post.postId]?.let {
                playableViewHolder.playMedia()
            }
        }
        onSnapAtAll?.let { it(item) }
        snapActionJob?.cancel()
        snapActionJob = scope.launch {
            delay(3000)
            val stillSnapped = recyclerView.layoutManager?.let { snapHelper.findSnapView(it) }
            val snappedPosNow = stillSnapped?.let { recyclerView.getChildAdapterPosition(it) }
                ?: RecyclerView.NO_POSITION
            if (snappedPosNow == pos) {
                onSnappedStable(item)
            }
        }
    }

    private fun setupFooterHeight() {
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val recyclerHeight = recyclerView.height
                val footerHeight = (recyclerHeight * 0.9f).toInt()
                adapter.setFooterHeight(footerHeight)
                recyclerView.adapter?.notifyItemChanged(adapter.items.size)
            }
        })
    }

    private fun bindMediaUpdates() {
        scope.launch {
            downloadMap.collect { newMap ->
                val visibleStart =
                    (recyclerView.layoutManager as? ScaledCarousel)?.findFirstVisibleItemPosition()
                        ?: return@collect
                val visibleEnd =
                    (recyclerView.layoutManager as? ScaledCarousel)?.findLastVisibleItemPosition()
                        ?: return@collect
                val newlyAddedKeys = newMap.keys - lastMediaMap.keys
                lastMediaMap = newMap
                for (i in visibleStart..visibleEnd) {
                    val item = adapter.items.getOrNull(i) ?: continue
                    if (item is TimelineItem.GistPost && item.post.postId in newlyAddedKeys) {
                        recyclerView.adapter?.notifyItemChanged(i)
                        if (item.post.postId == lastSnappedPostId &&
                            (item.post.postType == PostType.Video || item.post.postType == PostType.Audio)
                        ) {
                            recyclerView.post {
                                (recyclerView.findViewHolderForAdapterPosition(i) as? PlayableViewHolder)?.let { holder ->
                                    downloadMap.value[item.post.postId]?.let {
                                        holder.playMedia()
                                        lastSnappedViewHolder = holder
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun triggerSnapCheck() {
        recyclerView.post {
            val layoutManager = recyclerView.layoutManager as? ScaledCarousel ?: return@post
            val visibleStart = layoutManager.findFirstVisibleItemPosition()
            val visibleEnd = layoutManager.findLastVisibleItemPosition()

            for (i in visibleStart..visibleEnd) {
                val item = adapter.items.getOrNull(i) as? TimelineItem.GistPost ?: continue
                val type = item.post.postType
                if (type == PostType.Video || type == PostType.Audio) {
                    recyclerView.adapter?.notifyItemChanged(i)
                }
            }
            recyclerView.post { handleInitialSnap() }
        }
    }

    private fun listenToSnapping() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                val snappedView = rv.layoutManager?.let { snapHelper.findSnapView(it) } ?: return
                val pos = rv.getChildAdapterPosition(snappedView)
                val atTop = pos == 0
                onTopScroll?.let { it(atTop, { recyclerView.smoothScrollToPosition(0) }) }
                if (pos == RecyclerView.NO_POSITION || pos == lastSnapped) return

                val holder = rv.findViewHolderForAdapterPosition(pos) ?: return
                val playableViewHolder = holder as? PlayableViewHolder
                val item = adapter.items.getOrNull(holder.bindingAdapterPosition) ?: return

                viewStartTime?.let { start ->
                    val secondsViewed = ((System.currentTimeMillis() - start) / 1000).toInt()
                    if (lastSnappedPostId.isNotBlank() && secondsViewed > 0) {
                        onViewTimeRecorded(lastSnappedPostId, secondsViewed)
                    }
                }

                lastSnappedViewHolder?.pauseMedia()
                lastSnappedViewHolder = playableViewHolder
                lastSnappedPostId = (item as? TimelineItem.GistPost)?.post?.postId ?: ""
                lastSnapped = pos
                viewStartTime = System.currentTimeMillis()

                if (item is TimelineItem.GistPost &&
                    (item.post.postType == PostType.Video || item.post.postType == PostType.Audio)
                ) {
                    downloadMap.value[item.post.postId]?.let {
                        playableViewHolder?.playMedia()
                    }
                }
                onSnapAtAll?.let { it(item) }
                snapActionJob?.cancel()
                snapActionJob = scope.launch {
                    delay(3000)
                    val stillSnapped = rv.layoutManager?.let { snapHelper.findSnapView(it) }
                    val snappedPosNow = stillSnapped?.let { rv.getChildAdapterPosition(it) }
                        ?: RecyclerView.NO_POSITION
                    if (snappedPosNow == pos) {
                        onSnappedStable(item)
                    }
                }
            }
        })
    }

    fun setOnScrollDirectionListener(onScrollDirectionAdmission: (ScrollDirection) -> Unit) {
        var lastDy = 0
        val scrollForce = 5
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > scrollForce && lastDy <= 0) {
                    onScrollDirectionAdmission(ScrollDirection.ScrollUp)
                } else if (dy < -scrollForce && lastDy >= 0) {
                    onScrollDirectionAdmission(ScrollDirection.ScrollDown)
                }
                lastDy = dy
            }
        })
    }

    val snapAwareClickHandler: (Int, () -> Unit) -> Unit = { position, performAction ->
        if (position != lastSnapped) {
            recyclerView.smoothScrollToPosition(position)
        } else {
            performAction()
        }
    }
}