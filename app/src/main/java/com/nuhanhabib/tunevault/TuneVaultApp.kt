package com.nuhanhabib.tunevault

import android.app.Application
import android.content.Context
import com.nuhanhabib.tunevault.data.MusicRepository
import com.nuhanhabib.tunevault.data.YtDlp
import com.nuhanhabib.tunevault.data.db.AppDatabase
import com.nuhanhabib.tunevault.download.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TuneVaultApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this, AppDatabase.create(this).musicDao())
        DownloadService.createChannel(this)

        appScope.launch {
            runCatching { YtDlp.ensureInitialized(this@TuneVaultApp) }
            autoUpdateYtDlp()
        }
    }

    /** YouTube changes often, so keep yt-dlp fresh: try an update at most once a day. */
    private suspend fun autoUpdateYtDlp() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        if (System.currentTimeMillis() - last < 24 * 60 * 60 * 1000L) return
        runCatching { YtDlp.update(this) }.onSuccess {
            prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply()
        }
    }

    companion object {
        private const val KEY_LAST_UPDATE_CHECK = "last_ytdlp_update_check"
    }
}
