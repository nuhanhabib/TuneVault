package com.nuhanhabib.tunevault.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TrackStatus {
    const val PENDING = "PENDING"
    const val DOWNLOADING = "DOWNLOADING"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
}

/** A single song. [id] is the YouTube video id. */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val remoteThumbnail: String?,
    val filePath: String? = null,
    val thumbPath: String? = null,
    val status: String = TrackStatus.PENDING,
    val error: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)

/** A playlist imported from YouTube. [id] is the YouTube playlist id. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val remoteThumbnail: String?,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")],
)
data class PlaylistTrackRef(
    val playlistId: String,
    val trackId: String,
    val position: Int,
)

data class PlaylistSummary(
    @Embedded val playlist: PlaylistEntity,
    val total: Int,
    val downloaded: Int,
    val coverPath: String?,
)
