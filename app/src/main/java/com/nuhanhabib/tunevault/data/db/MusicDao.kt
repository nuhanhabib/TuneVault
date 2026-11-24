package com.nuhanhabib.tunevault.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // ---- Songs ----

    @Query("SELECT * FROM tracks WHERE status = 'DONE' ORDER BY addedAt DESC")
    fun observeDownloaded(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE status != 'DONE' ORDER BY addedAt ASC")
    fun observeQueue(): Flow<List<TrackEntity>>

    @Query("SELECT id FROM tracks")
    fun observeAllIds(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrack(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE status = 'PENDING' ORDER BY addedAt ASC LIMIT 1")
    suspend fun nextPending(): TrackEntity?

    @Query("UPDATE tracks SET status = :status, error = NULL WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("UPDATE tracks SET status = 'DONE', filePath = :filePath, thumbPath = :thumbPath, error = NULL WHERE id = :id")
    suspend fun markDone(id: String, filePath: String, thumbPath: String?)

    @Query("UPDATE tracks SET status = 'FAILED', error = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("UPDATE tracks SET status = 'PENDING' WHERE status = 'DOWNLOADING'")
    suspend fun resetInterrupted()

    @Query("UPDATE tracks SET status = 'PENDING', error = NULL WHERE status = 'FAILED'")
    suspend fun retryFailed()

    @Query("SELECT COUNT(*) FROM tracks WHERE status = 'PENDING' OR status = 'DOWNLOADING'")
    suspend fun pendingCount(): Int

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackRow(id: String)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :id")
    suspend fun deleteRefsForTrack(id: String)

    @Transaction
    suspend fun deleteTrack(id: String) {
        deleteRefsForTrack(id)
        deleteTrackRow(id)
    }

    // ---- Playlists ----

    @Query(
        """
        SELECT p.*,
               COUNT(pt.trackId) AS total,
               COALESCE(SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END), 0) AS downloaded,
               (SELECT t2.thumbPath FROM playlist_tracks pt2
                    JOIN tracks t2 ON t2.id = pt2.trackId
                    WHERE pt2.playlistId = p.id AND t2.thumbPath IS NOT NULL
                    ORDER BY pt2.position LIMIT 1) AS coverPath
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id
        LEFT JOIN tracks t ON t.id = pt.trackId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: String): PlaylistEntity?

    @Query(
        """
        SELECT t.* FROM tracks t
        JOIN playlist_tracks pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    fun observePlaylistTracks(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM playlists WHERE isLocal = 1 ORDER BY createdAt DESC")
    fun observeLocalPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT playlistId FROM playlist_tracks WHERE trackId = :trackId")
    fun observePlaylistIdsWith(trackId: String): Flow<List<String>>

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRef(ref: PlaylistTrackRef): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deleteRef(playlistId: String, trackId: String)

    @Query("UPDATE playlists SET title = :title WHERE id = :id")
    suspend fun renamePlaylist(id: String, title: String)

    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertRefs(refs: List<PlaylistTrackRef>)

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun trackIdsOf(playlistId: String): List<String>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun playlistCountOf(trackId: String): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun deleteRefsForPlaylist(id: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistRow(id: String)
}
