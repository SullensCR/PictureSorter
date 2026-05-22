package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val folderUri: String = "",
    val swipeDirection: String // "LEFT", "RIGHT", "UP", "NONE"
)
