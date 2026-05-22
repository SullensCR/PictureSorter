package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_photos")
data class PendingPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String, // Dynamic Uri, filepath or demo identifier
    val name: String,
    val size: String = "",
    val isSorted: Boolean = false,
    val isDeleted: Boolean = false,
    val sourceFolder: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)
