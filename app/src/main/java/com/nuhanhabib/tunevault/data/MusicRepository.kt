package com.nuhanhabib.tunevault.data

import android.content.Context
import android.os.Environment
import com.nuhanhabib.tunevault.data.db.MusicDao
import com.nuhanhabib.tunevault.data.db.PlaylistEntity
import com.nuhanhabib.tunevault.data.db.PlaylistTrackRef
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.download.DownloadService
import com.nuhanhabib.tunevault.download.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context, val dao: MusicDao) {

    /** App-private music folder. No storage permission needed. */
    val musicDir: File =
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "music")

    /** Adds a song or a whole playlist from a link and starts downloading. Returns a short message. */
    suspend fun addFromUrl(url: String): String {
        return when (val info = YtDlp.fetchInfo(context, url)) {
            is RemoteInfo.Single -> {
                val added = addTracks(listOf(info.track))
                startDownloads()
                if (added > 0) "Downloading \"${info.track.title}\"" else "\"${info.track.title}\" is already in your library"
            }
            is RemoteInfo.Playlist -> {
                val newOnes = savePlaylist(info, url)
                startDownloads()
                "Playlist \"${info.title}\": ${info.tracks.size} songs ($newOnes new)"
            }
        }
    }

    suspend fun addTrack(track: RemoteTrack): Boolean {
        val added = addTracks(listOf(track)) > 0
        startDownloads()
        return added
    }

    /** Re-reads a playlist from YouTube and queues any songs that were added since. */
    suspend fun syncPlaylist(playlistId: String): String {
        val playlist = dao.getPlaylist(playlistId) ?: return "Playlist not found"
        val info = YtDlp.fetchInfo(context, playlist.sourceUrl)
        if (info !is RemoteInfo.Playlist) return "This link is not a playlist anymore"
        val newOnes = savePlaylist(info, playlist.sourceUrl, playlist.createdAt)
        startDownloads()
        return if (newOnes == 0) "Already up to date" else "$newOnes new songs queued"
    }

    private suspend fun savePlaylist(
        info: RemoteInfo.Playlist,
        sourceUrl: String,
        createdAt: Long = System.currentTimeMillis(),
    ): Int {
        val newOnes = addTracks(info.tracks)
        dao.upsertPlaylist(
            PlaylistEntity(info.id, info.title, sourceUrl, info.thumbnail, createdAt)
        )
        dao.upsertRefs(info.tracks.mapIndexed { i, t -> PlaylistTrackRef(info.id, t.id, i) })
        return newOnes
    }

    /** Inserts tracks that are not already known. Returns how many were new. */
    private suspend fun addTracks(tracks: List<RemoteTrack>): Int {
        val now = System.currentTimeMillis()
        val distinct = tracks.distinctBy { it.id }
        val before = distinct.count { dao.getTrack(it.id) == null }
        dao.insertTracks(
            distinct.mapIndexed { i, t ->
                TrackEntity(
                    id = t.id,
                    title = t.title,
                    artist = t.artist,
                    durationSec = t.durationSec,
                    remoteThumbnail = t.thumbnail,
                    addedAt = now + i,
                )
            }
        )
        return before
    }

    fun startDownloads() = DownloadService.start(context)

    suspend fun retryFailed() {
        dao.retryFailed()
        startDownloads()
    }

    suspend fun deleteTrack(id: String) = withContext(Dispatchers.IO) {
        if (DownloadState.current.value?.trackId == id) YtDlp.cancel(id)
        dao.deleteTrack(id)
        musicDir.listFiles { f -> f.name.startsWith("$id.") }?.forEach { it.delete() }
    }

    /** Deletes a playlist. If [withSongs], songs that are not in any other playlist are deleted too. */
    suspend fun deletePlaylist(id: String, withSongs: Boolean) = withContext(Dispatchers.IO) {
        val trackIds = dao.trackIdsOf(id)
        dao.deleteRefsForPlaylist(id)
        dao.deletePlaylistRow(id)
        if (withSongs) {
            trackIds.filter { dao.playlistCountOf(it) == 0 }.forEach { deleteTrack(it) }
        }
    }

    fun storageUsedBytes(): Long = musicDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
