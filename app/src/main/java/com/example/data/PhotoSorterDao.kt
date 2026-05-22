package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoSorterDao {

    // --- Albums ---
    @Query("SELECT * FROM albums ORDER BY id ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums")
    suspend fun clearAlbums()


    // --- Pending Photos ---
    @Query("SELECT * FROM pending_photos WHERE isSorted = 0 AND isDeleted = 0 ORDER BY id ASC")
    fun getActivePendingPhotos(): Flow<List<PendingPhotoEntity>>

    @Query("SELECT * FROM pending_photos ORDER BY id ASC")
    fun getAllPendingPhotos(): Flow<List<PendingPhotoEntity>>

    @Query("SELECT COUNT(*) FROM pending_photos WHERE isSorted = 0 AND isDeleted = 0")
    fun getRemainingPhotosCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_photos")
    fun getTotalPhotosCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingPhotos(photos: List<PendingPhotoEntity>)

    @Update
    suspend fun updatePendingPhoto(photo: PendingPhotoEntity)

    @Query("UPDATE pending_photos SET isSorted = 0, isDeleted = 0 WHERE id = :photoId")
    suspend fun markPhotoAsUnsorted(photoId: Int)

    @Query("DELETE FROM pending_photos")
    suspend fun clearPendingPhotos()

    @Delete
    suspend fun deletePendingPhotos(photos: List<PendingPhotoEntity>)


    // --- Swipe History ---
    @Query("SELECT * FROM sorting_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Update
    suspend fun updateHistory(history: HistoryEntity)

    @Query("DELETE FROM sorting_history")
    suspend fun clearHistory()
}
