package com.justself.klique.gists.viewHolder

import android.content.Context
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.justself.klique.Logger
import com.justself.klique.R
import com.justself.klique.databinding.AudioPlayBinding
import com.justself.klique.databinding.GistImageDisplayBinding
import com.justself.klique.databinding.GistTextBinding
import com.justself.klique.databinding.GistVideoBinding
import com.justself.klique.databinding.ReplyingLayoutBinding
import com.justself.klique.gists.adapter.AudioPlayerPool
import com.justself.klique.gists.adapter.ExoPlayerPool
import com.justself.klique.gists.adapter.PlayableViewHolder
import com.justself.klique.gists.adapter.TimelineItem
import com.justself.klique.gists.adapter.truncateCount
import com.justself.klique.gists.data.models.Post
import com.justself.klique.gists.data.models.PostType
import com.justself.klique.nav.NavigationManager
import com.justself.klique.screenControllers.InnerGist
import com.justself.klique.useful_objects.ProfileImageObject
import kotlinx.coroutines.CoroutineScope
import java.io.File

class GistPageViewHolder(
    private val binding: ReplyingLayoutBinding,
    private val clickAction: (Int, () -> Unit) -> Unit,
    private val scope: CoroutineScope,
    private val nav: NavigationManager,
    private val context: Context,
    private val items: MutableList<TimelineItem>
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
            val gistContainer = binding.includedPost.gistContentContainer
            gistContainer.removeAllViews()

            val contentView = when (post.postType) {
                PostType.Text -> {
                    GistTextBinding.inflate(inflater, gistContainer, false).also {
                        it.textMessageBubble.text = post.text
                    }.root
                }

                PostType.Image -> {
                    GistImageDisplayBinding.inflate(inflater, gistContainer, false).also { root ->
                        file?.let { root.imageMessageView.setImageURI(Uri.fromFile(it)) }
                        root.captionsInclude.captionText.text = post.text
                    }.root
                }

                PostType.Video -> {
                    GistVideoBinding.inflate(inflater, gistContainer, false).also { view ->
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
                    AudioPlayBinding.inflate(inflater, gistContainer, false).also { view ->
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
                    GistTextBinding.inflate(inflater, gistContainer, false).also {
                        it.textMessageBubble.text =
                            context.getString(R.string.gist_type_unsupported_by_this_app_version)
                        it.textMessageBubble.setTypeface(
                            it.textMessageBubble.typeface,
                            Typeface.ITALIC
                        )
                    }.root
                }
            }
            binding.includedPost.gistContentContainer.addView(contentView)
        }
        with(binding.includedPost) {
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
            verifiedBadge.visibility = if (post.isVerified) View.VISIBLE else View.GONE
            namePlusImage.post {
                profileUnderline.layoutParams.width = namePlusImage.width
                profileUnderline.requestLayout()
            }
            commentCounts.text = returnOrganizedCount(post.commentCount, "comment")
        }
        if (bindingAdapterPosition == 0) {
            if (post.gistReply != null) {
                binding.replyContainer.visibility = View.VISIBLE
                binding.root.setOnClickListener(null)
                binding.replyType.text = when (post.gistReply.postType) {
                    PostType.Text -> {
                        "Text"
                    }

                    PostType.Image -> {
                        "Photo"
                    }

                    PostType.Video -> {
                        "Video"
                    }

                    PostType.Audio -> {
                        "Audio"
                    }

                    PostType.Unidentified -> {
                        "Unknown"
                    }
                }
                binding.replyBody.text = post.gistReply.text
                binding.replyQuoteContainer.setOnClickListener {
                    if (post.gistReply.postType != PostType.Unidentified) {
                        nav.goTo(InnerGist(nav, post.gistReply, context))
                    }
                }
            } else {
                binding.replyContainer.visibility = View.GONE
            }
        } else {
            binding.replyContainer.visibility = View.GONE
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    clickAction(position) {
                        if (post.postType == PostType.Unidentified) return@clickAction
                        val firstPost = (items.firstOrNull() as? TimelineItem.GistPost)?.post
                        val adjustedPost = post.copy(gistReply = firstPost)
                        nav.goTo(InnerGist(nav, adjustedPost, context))
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
            PostType.Video -> {
                Logger.d("FooterHeight", "videoplayer: ${videoPlayer == null}")
                videoPlayer?.playWhenReady = true
            }
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