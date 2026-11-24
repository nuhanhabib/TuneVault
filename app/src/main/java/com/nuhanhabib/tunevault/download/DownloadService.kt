package com.nuhanhabib.tunevault.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nuhanhabib.tunevault.R
import com.nuhanhabib.tunevault.TuneVaultApp
import com.nuhanhabib.tunevault.data.YtDlp
import com.nuhanhabib.tunevault.data.db.TrackEntity
import com.nuhanhabib.tunevault.data.db.TrackStatus
import com.nuhanhabib.tunevault.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveDownload(val trackId: String, val title: String, val progress: Float)

/** Live progress of the song that is downloading right now. */
object DownloadState {
    private val _current = MutableStateFlow<ActiveDownload?>(null)
    val current: StateFlow<ActiveDownload?> = _current.asStateFlow()
    internal fun set(value: ActiveDownload?) {
        _current.value = value
    }
}

/** Foreground service that downloads queued songs one after another, even with the screen off. */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var worker: Job? = null
    private var lastNotified = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Preparing downloads…", null),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        if (worker?.isActive != true) {
            worker = scope.launch {
                processQueue()
                DownloadState.set(null)
                ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun processQueue() {
        val repo = (application as TuneVaultApp).repository
        val dao = repo.dao
        dao.resetInterrupted()
        var completed = 0
        while (true) {
            val track = dao.nextPending() ?: break
            dao.setStatus(track.id, TrackStatus.DOWNLOADING)
            DownloadState.set(ActiveDownload(track.id, track.title, 0f))
            notifyProgress(track, 0f, force = true)
            try {
                val files = YtDlp.downloadAudio(this, track.id, repo.musicDir) { p ->
                    DownloadState.set(ActiveDownload(track.id, track.title, p))
                    notifyProgress(track, p)
                }
                if (dao.getTrack(track.id) == null) {
                    // Removed by the user while downloading.
                    files.audio.delete()
                    files.thumbnail?.delete()
                } else {
                    dao.markDone(track.id, files.audio.absolutePath, files.thumbnail?.absolutePath)
                    completed++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (dao.getTrack(track.id) != null) {
                    dao.markFailed(track.id, e.message?.lineSequence()?.lastOrNull { it.isNotBlank() } ?: "Download failed")
                }
            }
        }
        if (completed > 0) {
            NotificationManagerCompat.from(this).let { nm ->
                if (nm.areNotificationsEnabled()) {
                    nm.notify(
                        DONE_NOTIFICATION_ID,
                        baseBuilder()
                            .setContentTitle("Downloads finished")
                            .setContentText("$completed song(s) ready to play offline")
                            .setAutoCancel(true)
                            .build()
                    )
                }
            }
        }
    }

    private fun notifyProgress(track: TrackEntity, progress: Float, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotified < 1000) return
        lastNotified = now
        val nm = NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) {
            nm.notify(NOTIFICATION_ID, buildNotification(track.title, progress))
        }
    }

    private fun baseBuilder(): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
    }

    private fun buildNotification(text: String, progress: Float?): Notification =
        baseBuilder()
            .setContentTitle("Downloading music")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .apply {
                if (progress == null) setProgress(0, 0, true)
                else setProgress(100, (progress * 100).toInt(), false)
            }
            .build()

    override fun onDestroy() {
        DownloadState.set(null)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 1001
        private const val DONE_NOTIFICATION_ID = 1002

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java))
        }

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
