package com.example.data

import kotlinx.coroutines.flow.Flow

class PhotoSorterRepository(private val dao: PhotoSorterDao) {

    val allAlbums: Flow<List<AlbumEntity>> = dao.getAllAlbums()
    val activePendingPhotos: Flow<List<PendingPhotoEntity>> = dao.getActivePendingPhotos()
    val allPendingPhotos: Flow<List<PendingPhotoEntity>> = dao.getAllPendingPhotos()
    val remainingPhotosCount: Flow<Int> = dao.getRemainingPhotosCountFlow()
    val totalPhotosCount: Flow<Int> = dao.getTotalPhotosCountFlow()
    val sortingHistory: Flow<List<HistoryEntity>> = dao.getAllHistoryFlow()

    suspend fun insertAlbum(album: AlbumEntity): Long = dao.insertAlbum(album)
    suspend fun updateAlbum(album: AlbumEntity) = dao.updateAlbum(album)
    suspend fun deleteAlbum(album: AlbumEntity) = dao.deleteAlbum(album)
    suspend fun clearAlbums() = dao.clearAlbums()

    suspend fun insertPendingPhotos(photos: List<PendingPhotoEntity>) = dao.insertPendingPhotos(photos)
    suspend fun updatePendingPhoto(photo: PendingPhotoEntity) = dao.updatePendingPhoto(photo)
    suspend fun markPhotoAsUnsorted(photoId: Int) = dao.markPhotoAsUnsorted(photoId)
    suspend fun clearPendingPhotos() = dao.clearPendingPhotos()
    suspend fun deletePendingPhotos(photos: List<PendingPhotoEntity>) = dao.deletePendingPhotos(photos)

    suspend fun insertHistory(history: HistoryEntity): Long = dao.insertHistory(history)
    suspend fun updateHistory(history: HistoryEntity) = dao.updateHistory(history)
    suspend fun clearHistory() = dao.clearHistory()
}
