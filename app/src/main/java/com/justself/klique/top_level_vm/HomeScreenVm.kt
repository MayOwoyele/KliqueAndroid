package com.justself.klique.top_level_vm

import android.content.Context
import androidx.annotation.RawRes
import androidx.savedstate.serialization.serializers.MutableStateFlowSerializer
import com.justself.klique.BuildConfig
import com.justself.klique.Logger
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.R
import com.justself.klique.downloadFromUrl
import com.justself.klique.gists.adapter.TimelineItem
import com.justself.klique.gists.data.models.Post
import com.justself.klique.gists.data.models.PostType
import com.justself.klique.nav.KliqueVm
import com.justself.klique.nav.ViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

interface CarouselViewModel {
    val downloadedGistsMedia: StateFlow<Map<String, File>>
    fun handleSnappedPosStayed(item:TimelineItem)
    fun onPostViewTimeRecorded(thePostId: String, secondsViewed: Int)
    fun loadGistsFromServer()
    val timelineItems: StateFlow<List<TimelineItem>>
    fun downloadTheMedia(context: Context)
}
const val customCacheDirName = "my_temp_media"
class HomeScreenVm(screen: ViewController) : KliqueVm(screen), CarouselViewModel{
    private val _downloadedGistsMedia = MutableStateFlow<Map<String, File>>(mutableMapOf())
    override val downloadedGistsMedia = _downloadedGistsMedia.asStateFlow()
    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(mutableListOf())
    override val timelineItems = _timelineItems.asStateFlow()
    private val _triggerReload = MutableStateFlow(false)
    val triggerReload = _triggerReload.asStateFlow()
    init {
        clearCustomCacheDir()
    }
    fun setTrigger(bool: Boolean){
        _triggerReload.value = bool
    }
    override fun handleSnappedPosStayed(item: TimelineItem){
        Logger.d("Timeline", "The timeline item")
    }
    override fun onPostViewTimeRecorded(thePostId: String, secondsViewed: Int){
        Logger.d("Timeline", "Seconds viewed $secondsViewed")
    }
    private val dummyPosts = mutableListOf(
        Post("post_001", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Lanterns drift across woven constellations...", 2134, 0, 5,  PostType.Text, false, gistReply = Post("PostId", "Yamasutra", "pp", false, 5, "This is the post", 25, 5, 31, PostType.Text, false)),
        Post("post_002", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Whispers of time echo in the silence.", 134, 0, 25,  PostType.Text, false),
        Post("post_003", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Dancing memories shimmer in twilight.", 849, 3, 246236, PostType.Image, false, "https://example.com/media/image_1.jpg"),
        Post("post_004", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Beneath pastel umbrellas, dreams awaken.", 93, 1, 34,  PostType.Text, true),
        Post("post_005", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Chronicles hidden in dust of stars.", 10029424, 4, 2, PostType.Text, false),
        Post("post_006", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Moonlight sketches riddles across dew-kissed glass.", 1, 44000, 3, PostType.Unidentified, false),
        Post("post_007", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Echoes tumble through the forest's velvet hush.", 328, 4, 63, PostType.Text, false),
        Post("post_008", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Letters unsent drift through paper-laced winds.", 44, 4, 75, PostType.Text, false),
        Post("post_009", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Skyborne thoughts pirouette beyond the skyline.", 799, 4, 2, PostType.Video, false, "https://example.com/media/video_1.mp4"),
        Post("post_010", "May Owoyele", "https://example.com/profile.jpg", true, 1, "The sea hums lullabies to restless shadows.", 276, 4, 2, PostType.Text, true),
        Post("post_011", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Fleeting moments spiral through nostalgic corridors.", 612, 4, 2, PostType.Text, false),
        Post("post_012", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Light fractures like memory on rippling surfaces.", 192, 4, 6, PostType.Text, false),
        Post("post_013", "May Owoyele", "https://example.com/profile.jpg", true, 1, "A carousel", 87, 4, 6, PostType.Text, false),
        Post("post_014", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Roots remember what branches forget.", 411, 4, 6, PostType.Text, false),
        Post("post_015", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Dreams fold into corners of quiet notebooks.", 223, 4, 6, PostType.Text, false),
        Post("post_016", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Ink flows like thunder bottled in glass.", 369, 4, 6, PostType.Audio, true, "https://example.com/media/audio_1.mp3"),
        Post("post_017", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Stars blink Morse code into sleeping eyes.", 177, 4, 6, PostType.Text, false),
        Post("post_018", "May Owoyele", "https://example.com/profile.jpg", true, 1, "The hourglass breathes between silences.", 95, 4, 6, PostType.Text, false),
        Post("post_019", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Storms lull secrets in puddle reflections.", 518, 4, 6, PostType.Text, false),
        Post("post_020", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Every goodbye is a rewritten lullaby.", 146, 4, 6, PostType.Text, false),
        Post("post_021", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Hope flickers between static in the dark.", 631, 4, 6, PostType.Text, false),
        Post("post_022", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Gravity pulls thoughts into forgotten pages.", 302, 4, 6, PostType.Text, false),
        Post("post_023", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Curtains flutter like hesitant farewells.", 418, 4, 6, PostType.Text, false),
        Post("post_024", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Eyes sketch galaxies no telescope finds.", 274, 4, 6, PostType.Text, false),
        Post("post_025", "May Owoyele", "https://example.com/profile.jpg", true, 1, "Sunlight knits warmth through shattered clouds.", 390, 4, 6, PostType.Text, false)
    )
    private fun copyRawResourceToTempFile(
        context: Context,
        @RawRes rawResId: Int,
        fileName: String,
        subDir: String = "my_temp_media"
    ): File {
        val customCacheDir = File(context.cacheDir, subDir).apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File(customCacheDir, fileName)
        if (tempFile.exists()) return tempFile
        context.resources.openRawResource(rawResId).use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }
    override fun loadGistsFromServer(){
        val shapedDummyPosts = dummyPosts.map { TimelineItem.GistPost(it) }
        _timelineItems.value = shapedDummyPosts
    }
    override fun downloadTheMedia(context: Context){
        vmScope.launch(Dispatchers.IO) {
            delay(10000)
            val tempGist = mutableListOf<TimelineItem>()
            for (item in _timelineItems.value) {
                when (item){
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
    private fun clearCustomCacheDir() {
        val context = appContext
        val dir = File(context.cacheDir, customCacheDirName)
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
            dir.delete()
        }
    }
}
suspend fun actualDownloadAndMap(listOFItems: MutableList<TimelineItem>, context: Context, downloadedGistsMedia: MutableStateFlow<Map<String, File>>) {
    val tempMap = downloadedGistsMedia.value.toMutableMap()
    for (item in listOFItems){
        when (item){
            is TimelineItem.GistPost -> {
                when (item.post.postType) {
                    PostType.Video -> {
                        val file = downloadWrapper(context, item.post, "sample_video.mp4")
                        tempMap[item.post.postId] = file
                    }
                    PostType.Image -> {
                        val file = downloadWrapper(context, item.post, "sample_image.jpg")
                        tempMap[item.post.postId] = file
                    }
                    PostType.Audio -> {
                        val file = downloadWrapper(context, item.post, "sample_audio.m4a")
                        tempMap[item.post.postId] = file
                    }
                    PostType.Text -> {}
                    PostType.Unidentified -> {}
                }
            }
        }
    }
    downloadedGistsMedia.value = tempMap
}
suspend fun downloadWrapper(context: Context, post: Post, fileName: String): File {
    val customCacheDir = File(context.cacheDir, customCacheDirName).apply {
        if (!exists()) mkdirs()
    }
    val tempFile = File(customCacheDir, fileName)
    if (tempFile.exists()) return tempFile

    return if (BuildConfig.DEBUG) {
        val sampleRes = when (post.postType) {
            PostType.Video -> R.raw.sample_video
            PostType.Audio -> R.raw.sample_audio
            PostType.Image -> R.drawable.book_background
            else -> error("Unsupported media type")
        }
        context.resources.openRawResource(sampleRes).use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        tempFile
    } else {
        val mediaBytes = post.mediaLink?.let { downloadFromUrl(it) }
            ?: error("No mediaLink found for post ${post.postId}")
        tempFile.outputStream().use { output -> output.write(mediaBytes) }
        tempFile
    }
}