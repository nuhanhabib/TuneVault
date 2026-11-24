package com.nuhanhabib.tunevault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuhanhabib.tunevault.TuneVaultApp
import com.nuhanhabib.tunevault.data.RemoteTrack
import com.nuhanhabib.tunevault.data.YtDlp
import com.nuhanhabib.tunevault.data.db.PlaylistEntity
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.download.DownloadState
import com.nuhanhabib.tunevault.playback.PlayerConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as TuneVaultApp).repository
    private val dao = repo.dao
    val player = PlayerConnection(app)

    val songs = dao.observeDownloaded().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playlists = dao.observePlaylists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val queue = dao.observeQueue().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val knownIds = dao.observeAllIds().map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val activeDownload = DownloadState.current

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _searchResults = MutableStateFlow<List<RemoteTrack>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _ytDlpVersion = MutableStateFlow<String?>(null)
    val ytDlpVersion = _ytDlpVersion.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun playlistTracks(id: String): Flow<List<TrackEntity>> = dao.observePlaylistTracks(id)
    fun playlist(id: String): Flow<PlaylistEntity?> = dao.observePlaylist(id)

    /** Accepts a link, or plain text that is treated as a search. */
    fun submit(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        val url = URL_REGEX.find(text)?.value
        if (url != null) addUrl(url) else search(text)
    }

    private fun addUrl(url: String) = runBusy {
        _messages.send(repo.addFromUrl(url))
    }

    private fun search(query: String) = runBusy {
        _searchResults.value = YtDlp.search(getApplication(), query)
        if (_searchResults.value.isEmpty()) _messages.send("No results")
    }

    fun download(track: RemoteTrack) = viewModelScope.launch {
        if (repo.addTrack(track)) _messages.send("Downloading \"${track.title}\"")
    }

    fun play(tracks: List<TrackEntity>, index: Int = 0) = player.play(tracks, index)
    fun shuffle(tracks: List<TrackEntity>) = player.play(tracks, shuffle = true)

    fun deleteTrack(track: TrackEntity) = viewModelScope.launch {
        player.removeFromQueue(track.id)
        repo.deleteTrack(track.id)
    }

    fun deletePlaylist(id: String, withSongs: Boolean) = viewModelScope.launch {
        repo.deletePlaylist(id, withSongs)
    }

    fun syncPlaylist(id: String) = runBusy { _messages.send(repo.syncPlaylist(id)) }

    fun retryFailed() = viewModelScope.launch { repo.retryFailed() }
    fun resumeDownloads() = repo.startDownloads()

    fun loadYtDlpVersion() = viewModelScope.launch {
        _ytDlpVersion.value = runCatching { YtDlp.version(getApplication()) }.getOrNull()
    }

    fun updateYtDlp() = runBusy {
        val newVersion = YtDlp.update(getApplication())
        _messages.send(if (newVersion != null) "Updated to $newVersion" else "Already on the latest version")
        loadYtDlpVersion()
    }

    fun storageUsed(): Long = repo.storageUsedBytes()

    private fun runBusy(block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value = true
        try {
            block()
        } catch (e: Exception) {
            _messages.send(friendlyError(e))
        } finally {
            _busy.value = false
        }
    }

    private fun friendlyError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            "Unable to resolve host" in msg || "network" in msg.lowercase() -> "No internet connection"
            "Unsupported URL" in msg -> "This link is not supported"
            else -> msg.lineSequence().lastOrNull { it.isNotBlank() }?.take(160) ?: "Something went wrong"
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    companion object {
        private val URL_REGEX = Regex("https?://\\S+")
    }
}
