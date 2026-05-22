package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sorting_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val photoId: Int, // Refers to the original PendingPhotoEntity id
    val photoUri: String,
    val photoName: String,
    val actionType: String, // "SORT" or "DELETE"
    val targetAlbumId: Int = -1,
    val targetAlbumName: String = "",
    val swipeDirection: String = "NONE",
    val timestamp: Long = System.currentTimeMillis(),
    val isUndone: Boolean = false
)
