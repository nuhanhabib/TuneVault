package com.nuhanhabib.tunevault.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A song as seen on YouTube, before it is downloaded. */
data class RemoteTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnail: String?,
)

sealed interface RemoteInfo {
    data class Single(val track: RemoteTrack) : RemoteInfo
    data class Playlist(
        val id: String,
        val title: String,
        val thumbnail: String?,
        val tracks: List<RemoteTrack>,
    ) : RemoteInfo
}

data class DownloadedFiles(val audio: File, val thumbnail: File?)

/** Thin wrapper around the bundled yt-dlp binary. */
object YtDlp {

    private val initLock = Mutex()
    @Volatile private var initialized = false

    private val audioExtensions = setOf("m4a", "webm", "opus", "mp3", "ogg", "aac", "mp4", "mka")
    private val imageExtensions = setOf("webp", "jpg", "jpeg", "png")

    suspend fun ensureInitialized(context: Context) = withContext(Dispatchers.IO) {
        initLock.withLock {
            if (!initialized) {
                YoutubeDL.getInstance().init(context.applicationContext)
                initialized = true
            }
        }
    }

    suspend fun version(context: Context): String? = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        YoutubeDL.getInstance().version(context.applicationContext)
    }

    /** Updates yt-dlp to the newest stable release. Returns the new version, or null if already latest. */
    suspend fun update(context: Context): String? = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val status = YoutubeDL.getInstance()
            .updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel.STABLE)
        if (status == YoutubeDL.UpdateStatus.DONE) version(context) else null
    }

    /** Reads metadata for a video or playlist URL without downloading anything. */
    suspend fun fetchInfo(context: Context, url: String): RemoteInfo = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val request = YoutubeDLRequest(url).apply {
            addOption("--flat-playlist")
            addOption("--dump-single-json")
            addOption("--no-warnings")
        }
        val json = JSONObject(YoutubeDL.getInstance().execute(request).out)
        val entries = json.optJSONArray("entries")
        if (entries != null) {
            RemoteInfo.Playlist(
                id = json.optString("id").ifBlank { url.hashCode().toString() },
                title = json.optString("title").ifBlank { "Playlist" },
                thumbnail = bestThumbnail(json),
                tracks = parseEntries(entries),
            )
        } else {
            RemoteInfo.Single(parseTrack(json) ?: error("Could not read this video"))
        }
    }

    suspend fun search(context: Context, query: String, limit: Int = 25): List<RemoteTrack> =
        withContext(Dispatchers.IO) {
            ensureInitialized(context)
            val request = YoutubeDLRequest("ytsearch$limit:$query").apply {
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("--no-warnings")
            }
            val json = JSONObject(YoutubeDL.getInstance().execute(request).out)
            parseEntries(json.optJSONArray("entries") ?: JSONArray())
        }

    /**
     * Downloads only the audio stream of a video into [dir] as `<id>.<ext>`,
     * together with its thumbnail. No video is downloaded.
     */
    suspend fun downloadAudio(
        context: Context,
        videoId: String,
        dir: File,
        onProgress: (Float) -> Unit,
    ): DownloadedFiles = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        dir.mkdirs()
        dir.listFiles { f -> f.name.startsWith("$videoId.") }?.forEach { it.delete() }

        val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId").apply {
            addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
            addOption("--no-playlist")
            addOption("--match-filter", "!is_live")
            addOption("--write-thumbnail")
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("-o", File(dir, "$videoId.%(ext)s").absolutePath)
        }
        YoutubeDL.getInstance().execute(request, videoId) { progress, _, _ ->
            onProgress(progress.coerceIn(0f, 100f) / 100f)
        }

        val files = dir.listFiles { f -> f.name.startsWith("$videoId.") }.orEmpty()
        val audio = files.firstOrNull { it.extension.lowercase() in audioExtensions }
            ?: error("Audio file was not created")
        val thumb = files.firstOrNull { it.extension.lowercase() in imageExtensions }
        DownloadedFiles(audio, thumb)
    }

    fun cancel(videoId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(videoId) }
    }

    private fun parseEntries(entries: JSONArray): List<RemoteTrack> =
        (0 until entries.length()).mapNotNull { i -> entries.optJSONObject(i)?.let(::parseTrack) }

    private fun parseTrack(o: JSONObject): RemoteTrack? {
        val id = o.optString("id")
        val title = o.optString("title")
        if (id.isBlank() || id.length != 11) return null
        if (title == "[Private video]" || title == "[Deleted video]") return null
        // Live streams never end, so they can't be saved for offline listening.
        if (o.optString("live_status") in setOf("is_live", "is_upcoming", "post_live")) return null
        val artist = listOf("artist", "creator", "channel", "uploader")
            .map { o.optString(it) }
            .firstOrNull { it.isNotBlank() && it != "null" }
            .orEmpty()
            .removeSuffix(" - Topic")
        return RemoteTrack(
            id = id,
            title = title.ifBlank { id },
            artist = artist,
            durationSec = o.optDouble("duration", 0.0).let { if (it.isNaN()) 0L else it.toLong() },
            thumbnail = bestThumbnail(o) ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        )
    }

    private fun bestThumbnail(o: JSONObject): String? {
        o.optString("thumbnail").takeIf { it.startsWith("http") }?.let { return it }
        val thumbs = o.optJSONArray("thumbnails") ?: return null
        if (thumbs.length() == 0) return null
        return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")?.takeIf { it.startsWith("http") }
    }
}
