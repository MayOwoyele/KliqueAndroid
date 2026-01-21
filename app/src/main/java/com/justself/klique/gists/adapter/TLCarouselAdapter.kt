package com.justself.klique.gists.adapter

import android.content.Context
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.justself.klique.Logger
import com.justself.klique.R
import com.justself.klique.databinding.AudioPlayBinding
import com.justself.klique.databinding.GistImageDisplayBinding
import com.justself.klique.databinding.GistTextBinding
import com.justself.klique.databinding.GistVideoBinding
import com.justself.klique.databinding.PostItemBinding
import com.justself.klique.gists.data.models.Post
import com.justself.klique.gists.data.models.PostType
import com.justself.klique.nav.NavigationManager
import com.justself.klique.screenControllers.InnerGist
import com.justself.klique.useful_objects.ProfileImageObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface GistCarouselAdapter {
    fun setFooterHeight(height: Int)
    val items: MutableList<TimelineItem>
}

class TLCarouselAdapter(
    override val items: MutableList<TimelineItem> = mutableListOf(),
    private var footerHeight: Int = 0,
    private val downloadMap: StateFlow<Map<String, File>>,
    val context: Context,
    private val clickAction: (Int, () -> Unit) -> Unit,
    private val scope: CoroutineScope,
    val nav: NavigationManager
) : Adapter<RecyclerView.ViewHolder>(), GistCarouselAdapter {
    companion object {
        private const val TYPE_GIST = 0
        private const val TYPE_FOOTER = 99
    }

//    init {
//        setHasStableIds(true)
//    }
//
//    override fun getItemId(position: Int): Long {
//        return if (position < items.size) {
//            items[position].id
//        } else {
//            -1L
//        }
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GIST -> {
                val binding = PostItemBinding.inflate(inflater, parent, false)
                PostViewHolder(binding, clickAction, scope, nav, context)
            }

            TYPE_FOOTER -> {
                val emptyView = View(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        footerHeight
                    )
                }
                FooterViewHolder(emptyView)
            }

            else -> error("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return

        when {
            holder is PostViewHolder && item is TimelineItem.GistPost -> {
                val file = downloadMap.value[item.post.postId]
                holder.bind(item.post, file)
            }

            else -> {
                // no-op or log unexpected holder/item pair
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == items.size) return TYPE_FOOTER

        return when (items[position]) {
            is TimelineItem.GistPost -> TYPE_GIST
        }
    }

    override fun getItemCount(): Int = items.size + 1
    fun addItems(newItems: List<TimelineItem>) {
        val filtered = newItems.filter { new ->
            items.none { existing -> existing.id == new.id }
        }
        val start = items.size
        items.addAll(filtered)
        notifyItemRangeInserted(start, filtered.size)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        if (holder is PlayableViewHolder) {
            holder.pauseMedia()
        }
    }

    override fun setFooterHeight(height: Int) {
        footerHeight = height
    }
}

class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view)

class PostViewHolder(
    private val binding: PostItemBinding,
    private val clickAction: (Int, () -> Unit) -> Unit,
    private val scope: CoroutineScope,
    private val nav: NavigationManager,
    private val context: Context
) : RecyclerView.ViewHolder(binding.root), PlayableViewHolder {
    private var videoPlayer: ExoPlayer? = null
    private var audioPlayer: MediaPlayer? = null
    private var lastPostIdBound: String? = null
    private var currentPostType: PostType? = null
    private var audioBinding: AudioPlayBinding? = null
    private val seekHandler = Handler(Looper.getMainLooper())
    fun bind(post: Post, file: File?) {
        if (post.postId != lastPostIdBound) {
            releasePlayer()
            currentPostType = post.postType
            lastPostIdBound = post.postId

            val context = binding.root.context
            val inflater = LayoutInflater.from(context)
            binding.gistContentContainer.removeAllViews()

            val contentView = when (post.postType) {
                PostType.Text -> {
                    GistTextBinding.inflate(inflater, binding.gistContentContainer, false).also {
                        it.textMessageBubble.text = post.text
                    }.root
                }

                PostType.Image -> {
                    GistImageDisplayBinding.inflate(inflater, binding.gistContentContainer, false).also { root ->
                        file?.let { root.imageMessageView.setImageURI(Uri.fromFile(it)) }
                        root.captionsInclude.captionText.text = post.text
                    }.root
                }

                PostType.Video -> {
                    GistVideoBinding.inflate(inflater, binding.gistContentContainer, false).also { view ->
                        if (file == null) {
                            Logger.d("FileIsNull", "Well, file is null")
                            return@also
                        }
                        val uri = file.let { Uri.fromFile(it) }
                        val mediaItem = uri.let { MediaItem.fromUri(it) }
                        videoPlayer = ExoPlayerPool.getFresh(post.postId, context).apply {
                            setMediaItem(mediaItem)
                            prepare()
                            repeatMode = Player.REPEAT_MODE_ALL
                            playWhenReady = false
                        }
                        view.videoView.player = videoPlayer
                        view.captionInclude.captionText.text = post.text
                        val retriever = MediaMetadataRetriever().apply {
                            val retrieverUri = file.let { Uri.fromFile(it) }
                            retrieverUri?.let { setDataSource(context, it) }
                        }
                        val videoWidth = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                        )?.toIntOrNull() ?: 0
                        val videoHeight = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                        )?.toIntOrNull() ?: 0
                        retriever.release()
                        view.videoView.post {
                            if (videoWidth > 0 && videoHeight > 0) {
                                val screenWidth = view.videoView.width
                                val newHeight =
                                    (screenWidth * videoHeight / videoWidth.toFloat()).toInt()
                                view.videoView.layoutParams.height = newHeight
                                view.videoView.requestLayout()
                            }
                        }
                    }.root
                }

                PostType.Audio -> {
                    AudioPlayBinding.inflate(inflater, binding.gistContentContainer, false).also { view ->
                        view.audioSeekBar.apply {
                            isEnabled = false
                            isClickable = false
                            isFocusable = false
                        }
                        audioBinding = view
                        val playerId = post.postId
                        AudioPlayerPool.release(playerId)
                        val player = AudioPlayerPool.getFresh(playerId)
                        if (file != null && file.exists()) {
                            player.setDataSource(context, Uri.fromFile(file))
                            player.isLooping = true
                            player.prepare()
                            audioPlayer = player
                            view.audioSeekBar.progress = 0
                            view.audioSeekBar.max = 100
                            view.audioDuration.text = formatDuration(player.duration)
                            view.audioPlayPause.setImageResource(R.drawable.play_arrow_24px)
                        }
                        view.captionsInclude.captionText.text = post.text
                    }.root
                }

                PostType.Unidentified -> {
                    GistTextBinding.inflate(inflater, binding.gistContentContainer, false).also {
                        it.textMessageBubble.text =
                            context.getString(R.string.gist_type_unsupported_by_this_app_version)
                        it.textMessageBubble.setTypeface(
                            it.textMessageBubble.typeface,
                            Typeface.ITALIC
                        )
                    }.root
                }
            }
            binding.gistContentContainer.addView(contentView)
        }
        with(binding) {
            viewsCount.text = returnOrganizedCount(post.views, "view")
            superViews.text = returnOrganizedCount(post.superViews, "superview")
            userName.text = post.name
            val expectedTag = post.userId.toString()
            profileImage.tag = expectedTag
            ProfileImageObject.getOrDownloadThumbnail(
                post.userId,
                post.profileImage,
                scope = scope
            ) {
                if (profileImage.tag == expectedTag) {
                    val uri = Uri.fromFile(it)
                    profileImage.setImageURI(uri)
                }
            }
            commentCounts.text = returnOrganizedCount(post.commentCount, "comment")
            verifiedBadge.visibility = if (post.isVerified) View.VISIBLE else View.GONE
            namePlusImage.post {
                profileUnderline.layoutParams.width = namePlusImage.width
                profileUnderline.requestLayout()
            }
            root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    clickAction(position) {
                        if (post.postType != PostType.Unidentified) {
                            nav.goTo(InnerGist(nav, post, context))
                        }
                    }
                }
            }
        }
    }

    private fun returnOrganizedCount(number: Int, suffix: String): String {
        return when {
            number < 1 -> "No ${suffix}s"
            number == 1 -> "A $suffix"
            else -> "${truncateCount(number)} ${suffix}s"
        }
    }

    override fun playMedia() {
        when (currentPostType) {
            PostType.Video -> videoPlayer?.playWhenReady = true
            PostType.Audio -> {
                try {
                    audioPlayer?.start()
                    audioBinding?.audioPlayPause?.setImageResource(R.drawable.pause_24px)
                    startSeekBarUpdates()
                } catch (e: IllegalStateException) {
                    Logger.e("MediaPlayer", "Crash in playMedia(): ${e.message}")
                    stopSeekBarUpdates()
                }
            }

            else -> Unit
        }
    }

    override fun pauseMedia() {
        when (currentPostType) {
            PostType.Video -> videoPlayer?.playWhenReady = false
            PostType.Audio -> {
                try {
                    if (audioPlayer?.isPlaying == true) {
                        audioPlayer?.pause()
                        audioBinding?.audioPlayPause?.setImageResource(R.drawable.play_arrow_24px)
                        stopSeekBarUpdates()
                    }
                } catch (e: IllegalStateException) {
                    Logger.e("MediaPlayer", "Tried to pause in invalid state: ${e.message}")
                    // Optionally: reset or release
                }
            }

            else -> Unit
        }
    }

    private fun releasePlayer() {
        stopSeekBarUpdates()
        videoPlayer?.release()
        videoPlayer = null
        audioPlayer?.release()
        audioPlayer = null
        audioBinding = null
    }

    private fun formatDuration(ms: Int): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val remSeconds = seconds % 60
        return "$minutes:${remSeconds.toString().padStart(2, '0')}"
    }

    private fun startSeekBarUpdates() {
        seekHandler.post(object : Runnable {
            override fun run() {
                val player = audioPlayer
                val binding = audioBinding

                if (player == null || binding == null) return

                try {
                    if (player.isPlaying) {
                        val progressPercent = (player.currentPosition * 100) / player.duration
                        binding.audioSeekBar.progress = progressPercent
                        binding.audioDuration.text = formatDuration(player.currentPosition)
                        seekHandler.postDelayed(this, 1000)
                    } else {
                        seekHandler.postDelayed(this, 1000)
                    }
                } catch (e: IllegalStateException) {
                    Logger.e("MediaPlayer", "Caught crash in startSeekBarUpdates: ${e.message}")
                    stopSeekBarUpdates()
                }
            }
        })
    }

    private fun stopSeekBarUpdates() {
        seekHandler.removeCallbacksAndMessages(null)
    }
}

sealed class TimelineItem {
    abstract val id: Long

    data class GistPost(val post: Post) : TimelineItem() {
        override val id: Long get() = post.postId.hashCode().toLong()
    }
}

fun truncateCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000.0).format(1)}M"
        count >= 1_000 -> "${(count / 1_000.0).format(1)}K"
        else -> count.toString()
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this).removeSuffix(".0")

object ExoPlayerPool {
    private const val MAX_PLAYERS = 3
    private val activePlayers = LinkedHashMap<String, ExoPlayer>()

    fun getFresh(playerId: String, context: Context): ExoPlayer {
        // Clean up if the player already exists
        activePlayers[playerId]?.let { existing ->
            existing.release()
            activePlayers.remove(playerId)
        }

        // Evict the oldest if at max capacity
        if (activePlayers.size >= MAX_PLAYERS) {
            val oldestKey = activePlayers.entries.first().key
            activePlayers[oldestKey]?.release()
            activePlayers.remove(oldestKey)
        }

        // Create a fresh new player
        val newPlayer = ExoPlayer.Builder(context).build()
        activePlayers[playerId] = newPlayer
        return newPlayer
    }

    fun release(playerId: String) {
        activePlayers[playerId]?.release()
        activePlayers.remove(playerId)
    }

    fun releaseAll() {
        activePlayers.values.forEach { it.release() }
        activePlayers.clear()
    }
}

object AudioPlayerPool {
    private const val MAX_PLAYERS = 5
    private val activePlayers = LinkedHashMap<String, MediaPlayer>()

    fun getFresh(playerId: String): MediaPlayer {
        activePlayers[playerId]?.let { existing ->
            existing.release()
            activePlayers.remove(playerId)
        }

        if (activePlayers.size >= MAX_PLAYERS) {
            val oldestKey = activePlayers.entries.first().key
            activePlayers[oldestKey]?.release()
            activePlayers.remove(oldestKey)
        }

        val newPlayer = MediaPlayer()
        activePlayers[playerId] = newPlayer
        return newPlayer
    }

    fun release(playerId: String) {
        activePlayers[playerId]?.release()
        activePlayers.remove(playerId)
    }

    fun releaseAll() {
        activePlayers.values.forEach { it.release() }
        activePlayers.clear()
    }
}
interface PlayableViewHolder{
    fun playMedia()
    fun pauseMedia()
}