package com.example.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SetupPhase {
    CHOOSE_SOURCE,
    CHOOSE_TARGETS,
    CHOOSE_SWIPES,
    ACTIVE_SORTING
}

data class DebugLogEntry(
    val id: Int,
    val timestamp: String,
    val action: String,
    val status: String,
    val message: String
)

data class MediaFolder(
    val id: String,
    val name: String,
    val count: Int
)

class PhotoSorterViewModel(
    private val repository: PhotoSorterRepository,
    private val context: Context
) : ViewModel() {

    private val _debugLogs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val debugLogs: StateFlow<List<DebugLogEntry>> = _debugLogs.asStateFlow()
    private var nextLogId = 1

    fun addDebugLog(action: String, status: String, message: String) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        val time = sdf.format(java.util.Date())
        val entry = DebugLogEntry(nextLogId++, time, action, status, message)
        _debugLogs.value = _debugLogs.value + entry
    }

    fun clearDebugLogs() {
        _debugLogs.value = emptyList()
    }

    // List of detected physical device directory buckets
    private val _detectedFolders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val detectedFolders: StateFlow<List<MediaFolder>> = _detectedFolders.asStateFlow()

    // Configured bucket-ID of the currently active source photo folder
    private val _selectedFolderId = MutableStateFlow<String>("ALL")
    val selectedFolderId: StateFlow<String> = _selectedFolderId.asStateFlow()

    // Toggle for infinite sorting (load up to 10k photos) vs batch-limiting (50 photos)
    private val _useInfiniteMode = MutableStateFlow<Boolean>(true)
    val useInfiniteMode: StateFlow<Boolean> = _useInfiniteMode.asStateFlow()

    // Current phase of the app setup
    private val _setupPhase = MutableStateFlow(SetupPhase.CHOOSE_SOURCE)
    val setupPhase: StateFlow<SetupPhase> = _setupPhase.asStateFlow()

    // Screen tab: 0 for Workspace, 1 for History
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Loading indicator for processing gallery or populating data
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // List of albums configured by user
    val albumsFlow: StateFlow<List<AlbumEntity>> = repository.allAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Remaining images dynamically updated from Room
    val activePhotosFlow: StateFlow<List<PendingPhotoEntity>> = repository.activePendingPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total and remaining counts for progress tracking
    val remainingPhotosCount: StateFlow<Int> = repository.remainingPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPhotosCount: StateFlow<Int> = repository.totalPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // History log for viewing and undoing actions
    val historyLog: StateFlow<List<HistoryEntity>> = repository.sortingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Read list of trashed photos (where isDeleted is true)
    val trashedPhotosFlow: StateFlow<List<PendingPhotoEntity>> = repository.allPendingPhotos
        .map { photos -> photos.filter { it.isDeleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message toaster helper state
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // Automatically check if configuration already exists to resume active sorting on start
        viewModelScope.launch {
            try {
                val albums = repository.allAlbums.first()
                val photos = repository.allPendingPhotos.first()
                if (albums.isNotEmpty() && photos.isNotEmpty()) {
                    _setupPhase.value = SetupPhase.ACTIVE_SORTING
                } else {
                    _setupPhase.value = SetupPhase.CHOOSE_SOURCE
                }
            } catch (e: Exception) {
                _setupPhase.value = SetupPhase.CHOOSE_SOURCE
            }
        }
    }

    // --- DIRECTORY & PERMISSION HELPERS ---

    fun getAlbumsBaseDir(): java.io.File {
        return try {
            val base = context.externalMediaDirs.firstOrNull() ?: context.getExternalFilesDir(null) ?: context.filesDir
            val dir = java.io.File(base, "Albums")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (e: Exception) {
            Log.e("PhotoSorterViewModel", "Error creating external base albums dir, falling back to filesDir", e)
            val dir = java.io.File(context.filesDir, "Albums")
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            } catch (ex: Exception) {
                Log.e("PhotoSorterViewModel", "Failed to create fallback Albums dir", ex)
            }
            dir
        }
    }

    fun getTrashDir(): java.io.File {
        return try {
            val base = context.externalMediaDirs.firstOrNull() ?: context.getExternalFilesDir(null) ?: context.filesDir
            val dir = java.io.File(base, "Trash")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (e: Exception) {
            Log.e("PhotoSorterViewModel", "Error creating external trash dir, falling back to filesDir", e)
            val dir = java.io.File(context.filesDir, "Trash")
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            } catch (ex: Exception) {
                Log.e("PhotoSorterViewModel", "Failed to create fallback Trash dir", ex)
            }
            dir
        }
    }

    fun checkFolderExists(path: String): Boolean {
        if (path.isEmpty()) return false
        return try {
            java.io.File(path).exists()
        } catch (e: Exception) {
            Log.e("PhotoSorterViewModel", "Error checking if folder exists: $path", e)
            false
        }
    }

    fun createFolder(path: String) {
        if (path.isNotEmpty()) {
            try {
                val dir = java.io.File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            } catch (e: Exception) {
                Log.e("PhotoSorterViewModel", "Failed to create directory: $path", e)
            }
        }
    }

    private fun deleteOriginalSource(path: String) {
        try {
            if (path.startsWith("/")) {
                val f = java.io.File(path)
                if (f.exists()) {
                    f.delete()
                }
            } else if (path.startsWith("file://")) {
                val f = java.io.File(java.net.URI(path))
                if (f.exists()) {
                    f.delete()
                }
            } else if (path.startsWith("content://")) {
                val uri = android.net.Uri.parse(path)
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: Exception) {
            Log.e("PhotoSorterViewModel", "Failed to delete original source at: $path", e)
        }
    }

    private fun restoreOriginalSource(fromFile: java.io.File, originalUriString: String) {
        if (!fromFile.exists()) return
        try {
            if (originalUriString.startsWith("/")) {
                val destFile = java.io.File(originalUriString)
                destFile.parentFile?.mkdirs()
                fromFile.copyTo(destFile, overwrite = true)
            } else if (originalUriString.startsWith("file://")) {
                val destFile = java.io.File(java.net.URI(originalUriString))
                destFile.parentFile?.mkdirs()
                fromFile.copyTo(destFile, overwrite = true)
            } else if (originalUriString.startsWith("content://")) {
                val uri = android.net.Uri.parse(originalUriString)
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        java.io.FileInputStream(fromFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PhotoSorterViewModel", "Could not write to content URI on undo. Re-inserting to MediaStore.", e)
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fromFile.name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    }
                    val newUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (newUri != null) {
                        context.contentResolver.openOutputStream(newUri)?.use { output ->
                            java.io.FileInputStream(fromFile).use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoSorterViewModel", "Failed to restore original file on undo", e)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    // --- SETUP PHASE ACTIONS ---

    // Load gorgeous default demo photos in "Sandbox Mode"
    fun loadDemoPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.clearPendingPhotos()

            val demoPhotos = listOf(
                PendingPhotoEntity(path = "demo://mount_fuji", name = "Mount Fuji Sunset.jpg", size = "1.2 MB"),
                PendingPhotoEntity(path = "demo://bora_bora", name = "Bora Bora Beach.png", size = "2.4 MB"),
                PendingPhotoEntity(path = "demo://neon_tokyo", name = "Neon Tokyo Nightlife.jpg", size = "3.1 MB"),
                PendingPhotoEntity(path = "demo://desert_dunes", name = "Golden Sahara Desert.png", size = "1.8 MB"),
                PendingPhotoEntity(path = "demo://nordic_cabin", name = "Nordic Winter Cabin.jpg", size = "2.2 MB"),
                PendingPhotoEntity(path = "demo://misty_bridge", name = "Misty Golden Gate.png", size = "2.9 MB"),
                PendingPhotoEntity(path = "demo://emerald_lake", name = "Emerald Pine Lake.jpg", size = "1.5 MB"),
                PendingPhotoEntity(path = "demo://retro_cafe", name = "Retro Cafe Record Player.png", size = "1.1 MB"),
                PendingPhotoEntity(path = "demo://cosmic_aurora", name = "Northern Cosmic Lights.jpg", size = "3.5 MB"),
                PendingPhotoEntity(path = "demo://autumn_park", name = "Autumn Maple Walkway.png", size = "2.0 MB"),
                PendingPhotoEntity(path = "demo://cute_kitten", name = "Playful Kitten.jpg", size = "950 KB"),
                PendingPhotoEntity(path = "demo://vintage_car", name = "Classic Red Convertible.png", size = "2.8 MB")
            )

            repository.insertPendingPhotos(demoPhotos)
            _isLoading.value = false
            _message.value = "Loaded 12 Sandbox Demo Images!"
            _setupPhase.value = SetupPhase.CHOOSE_TARGETS
        }
    }

    fun setInfiniteMode(enabled: Boolean) {
        _useInfiniteMode.value = enabled
    }

    // Scan for all directory buckets containing images on the user's device
    fun scanMediaFolders(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val foldersList = mutableListOf<MediaFolder>()
                val projection = arrayOf(
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                try {
                    val cursor = context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                    cursor?.use { cu ->
                        val bucketIdCol = cu.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                        val bucketNameCol = cu.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                        
                        val folderCounts = mutableMapOf<String, Int>()
                        val folderNames = mutableMapOf<String, String>()
                        
                        while (cu.moveToNext()) {
                            val id = if (bucketIdCol != -1) cu.getString(bucketIdCol) else null
                            val name = if (bucketNameCol != -1) cu.getString(bucketNameCol) ?: "Unnamed Folder" else "Gallery"
                            if (id != null) {
                                folderCounts[id] = (folderCounts[id] ?: 0) + 1
                                folderNames[id] = name
                            }
                        }
                        
                        folderCounts.forEach { (id, count) ->
                            foldersList.add(MediaFolder(id = id, name = folderNames[id] ?: "Unknown", count = count))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PhotoSorterViewModel", "Error fetching folders buckets", e)
                }
                
                foldersList.sortByDescending { it.count }
                
                val totalCount = foldersList.sumOf { it.count }
                val merged = mutableListOf<MediaFolder>().apply {
                    add(MediaFolder(id = "ALL", name = "All Device Photos", count = totalCount))
                    addAll(foldersList)
                }
                _detectedFolders.value = merged
            }
        }
    }

    // Scan the user's real device MediaStore Gallery
    fun scanDevicePhotos(
        context: Context,
        bucketId: String? = null,
        infinite: Boolean? = null,
        append: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val isInfinite = infinite ?: _useInfiniteMode.value
            if (bucketId != null) {
                _selectedFolderId.value = bucketId
            }
            val activeBucket = bucketId ?: _selectedFolderId.value

            val success = withContext(Dispatchers.IO) {
                val list = mutableListOf<PendingPhotoEntity>()
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                val selection = if (activeBucket != "ALL" && activeBucket.isNotEmpty()) {
                    "${MediaStore.Images.Media.BUCKET_ID} = ?"
                } else {
                    null
                }
                val selectionArgs = if (activeBucket != "ALL" && activeBucket.isNotEmpty()) {
                    arrayOf(activeBucket)
                } else {
                    null
                }

                try {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                        var count = 0
                        val limit = if (isInfinite) 10000 else 50
                        while (cursor.moveToNext() && count < limit) {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn) ?: "Image_$id"
                            val sizeBytes = cursor.getLong(sizeColumn)
                            val sizeStr = if (sizeBytes > 0) "${String.format("%.1f", sizeBytes / 1024.0 / 1024.0)} MB" else "Unknown Size"

                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            list.add(
                                PendingPhotoEntity(
                                    path = contentUri.toString(),
                                    name = name,
                                    size = sizeStr
                                )
                            )
                            count++
                        }
                    }
                    if (list.isNotEmpty()) {
                        if (!append) {
                            repository.clearPendingPhotos()
                        }
                        repository.insertPendingPhotos(list)
                        addDebugLog(
                            action = "SCAN_GALLERY",
                            status = "SUCCESS",
                            message = "${if (append) "Appended" else "Imported"} ${list.size} photos from folder '${activeBucket}'"
                        )
                        true
                    } else {
                        addDebugLog(
                            action = "SCAN_GALLERY",
                            status = "WARNING",
                            message = "No photos found in selected directory source."
                        )
                        false
                    }
                } catch (e: Exception) {
                    Log.e("PhotoSorterViewModel", "Error reading MediaStore", e)
                    addDebugLog(
                        action = "SCAN_GALLERY",
                        status = "FAILURE",
                        message = "Failed to scan folder of images: ${e.message}"
                    )
                    false
                }
            }

            _isLoading.value = false
            if (success) {
                _message.value = if (append) "Appended photos successfully!" else "Imported batch from folder successfully!"
                if (_setupPhase.value == SetupPhase.CHOOSE_SOURCE) {
                    _setupPhase.value = SetupPhase.CHOOSE_TARGETS
                }
            } else if (!append) {
                // If device gallery is empty or permission failed, fallback gracefully (only on initial select)
                if (_setupPhase.value == SetupPhase.CHOOSE_SOURCE) {
                    _message.value = "No device photos found. Loading Sandbox Demo Photos instead!"
                    loadDemoPhotos()
                } else {
                    _message.value = "Selected folder is empty or inaccessible"
                }
            }
        }
    }

    // Save album targets (either 2, 3, or 4) and physically prepare target folders on device
    fun setTargetAlbums(album1: String, album2: String, album3: String?, album4: String?) {
        viewModelScope.launch {
            repository.clearAlbums()

            val baseDir = getAlbumsBaseDir()
            val path1 = java.io.File(baseDir, album1.trim()).absolutePath
            val path2 = java.io.File(baseDir, album2.trim()).absolutePath

            withContext(Dispatchers.IO) {
                java.io.File(path1).mkdirs()
                java.io.File(path2).mkdirs()
            }

            val a1 = AlbumEntity(name = album1.trim(), swipeDirection = "LEFT", folderUri = path1)
            val a2 = AlbumEntity(name = album2.trim(), swipeDirection = "RIGHT", folderUri = path2)
            repository.insertAlbum(a1)
            repository.insertAlbum(a2)

            if (!album3.isNullOrBlank()) {
                val path3 = java.io.File(baseDir, album3.trim()).absolutePath
                withContext(Dispatchers.IO) {
                    java.io.File(path3).mkdirs()
                }
                val a3 = AlbumEntity(name = album3.trim(), swipeDirection = "UP", folderUri = path3)
                repository.insertAlbum(a3)
            }

            if (!album4.isNullOrBlank()) {
                val path4 = java.io.File(baseDir, album4.trim()).absolutePath
                withContext(Dispatchers.IO) {
                    java.io.File(path4).mkdirs()
                }
                val a4 = AlbumEntity(name = album4.trim(), swipeDirection = "DOWN", folderUri = path4)
                repository.insertAlbum(a4)
            }

            _setupPhase.value = SetupPhase.CHOOSE_SWIPES
        }
    }

    // Confirm custom mapped swipe direction setup
    fun confirmSwipeMappings() {
        _setupPhase.value = SetupPhase.ACTIVE_SORTING
        _message.value = "Sorting ready! Swipe away."
    }

    // Reset whole workspace configuration and start over
    fun resetAppConfiguration() {
        viewModelScope.launch {
            repository.clearAlbums()
            repository.clearPendingPhotos()
            repository.clearHistory()
            _setupPhase.value = SetupPhase.CHOOSE_SOURCE
            _currentTab.value = 0
            _message.value = "Application was reset."
        }
    }

    // --- SORTING ACTIONS ---

    // Sort active top photo to a specific folder: copies/moves file to physical location
    fun swipePhoto(photo: PendingPhotoEntity, direction: String) {
        viewModelScope.launch {
            val albums = albumsFlow.value
            val matchedAlbum = albums.find { it.swipeDirection.uppercase() == direction.uppercase() }

            if (matchedAlbum != null) {
                var verifySuccess = false
                var errorMsg: String? = null
                withContext(Dispatchers.IO) {
                    try {
                        val albDir = java.io.File(matchedAlbum.folderUri)
                        if (!albDir.exists()) {
                            albDir.mkdirs()
                        }
                        val destFile = java.io.File(albDir, photo.name)
                        if (photo.path.startsWith("demo://")) {
                            // Generate a beautiful, openable placeholder image with details
                            val b = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(b)
                            val paint = android.graphics.Paint()
                            paint.color = android.graphics.Color.HSVToColor(floatArrayOf((photo.id * 57 % 360).toFloat(), 0.8f, 0.8f))
                            canvas.drawRect(0f, 0f, 800f, 600f, paint)

                            paint.color = android.graphics.Color.WHITE
                            paint.textSize = 34f
                            paint.isAntiAlias = true
                            paint.textAlign = android.graphics.Paint.Align.CENTER
                            canvas.drawText(photo.name, 400f, 260f, paint)

                            paint.textSize = 24f
                            canvas.drawText("Sorted to Album: ${matchedAlbum.name}", 400f, 320f, paint)
                            canvas.drawText("Folder: ${albDir.name}", 400f, 370f, paint)

                            java.io.FileOutputStream(destFile).use { out ->
                                b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            b.recycle()
                        } else {
                            // Copy real gallery file using ContentResolver helper
                            val uri = android.net.Uri.parse(photo.path)
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            deleteOriginalSource(photo.path)
                        }

                        // Verify physical file was written and is not empty
                        if (destFile.exists() && destFile.length() > 0) {
                            verifySuccess = true
                        } else {
                            errorMsg = "Destination file not found or is empty after write: ${destFile.absolutePath}"
                        }
                    } catch (e: Exception) {
                        Log.e("PhotoSorterViewModel", "Error physically moving swiped file", e)
                        errorMsg = e.message ?: "Unknown I/O exception during copy/write"
                    }
                }

                // Add debug/verification logs
                if (verifySuccess) {
                    addDebugLog(
                        action = "SWIPE/SORT",
                        status = "SUCCESS",
                        message = "Photo '${photo.name}' successfully moved to album '${matchedAlbum.name}' folder"
                    )
                } else {
                    addDebugLog(
                        action = "SWIPE/SORT",
                        status = "FAILURE",
                        message = "Verification failed for '${photo.name}': $errorMsg"
                    )
                }

                // Regular Sort to Album folder
                val updatedPhoto = photo.copy(isSorted = true)
                repository.updatePendingPhoto(updatedPhoto)

                val history = HistoryEntity(
                    photoId = photo.id,
                    photoUri = photo.path,
                    photoName = photo.name,
                    actionType = "SORT",
                    targetAlbumId = matchedAlbum.id,
                    targetAlbumName = matchedAlbum.name,
                    swipeDirection = direction
                )
                repository.insertHistory(history)
                _message.value = "Moved to ${matchedAlbum.name}"
            } else {
                // Unknown swipe direction mapping, do nothing
                Log.w("PhotoSorterViewModel", "Unhandled direction swiped: $direction")
                addDebugLog(
                    action = "SWIPE/SORT",
                    status = "FAILURE",
                    message = "No configured album matches the swiped direction: $direction"
                )
            }
        }
    }

    // Delete photo - moves to a dedicated Trash Bin directory
    fun deletePhotoPermanently(photo: PendingPhotoEntity) {
        viewModelScope.launch {
            var verifySuccess = false
            var errorMsg: String? = null
            withContext(Dispatchers.IO) {
                try {
                    val trashDir = getTrashDir()
                    if (!trashDir.exists()) {
                        trashDir.mkdirs()
                    }
                    val destFile = java.io.File(trashDir, photo.name)
                    if (photo.path.startsWith("demo://")) {
                        // Generate a dark red trashed placeholder image
                        val b = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(b)
                        val paint = android.graphics.Paint()
                        paint.color = android.graphics.Color.parseColor("#3a1010")
                        canvas.drawRect(0f, 0f, 800f, 600f, paint)

                        paint.color = android.graphics.Color.parseColor("#FF6B6B")
                        paint.textSize = 34f
                        paint.isAntiAlias = true
                        paint.textAlign = android.graphics.Paint.Align.CENTER
                        canvas.drawText("TRASHED: ${photo.name}", 400f, 260f, paint)

                        paint.color = android.graphics.Color.GRAY
                        paint.textSize = 22f
                        canvas.drawText("Awaiting Confirmed Deletion", 400f, 330f, paint)

                        java.io.FileOutputStream(destFile).use { out ->
                            b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        b.recycle()
                    } else {
                        // Copy actual image to trash files directory
                        val uri = android.net.Uri.parse(photo.path)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        deleteOriginalSource(photo.path)
                    }

                    // Verify trash file was written and is not empty
                    if (destFile.exists() && destFile.length() > 0) {
                        verifySuccess = true
                    } else {
                        errorMsg = "Trash file not found or is empty after write: ${destFile.absolutePath}"
                    }
                } catch (e: Exception) {
                    Log.e("PhotoSorterViewModel", "Error physically moving file to trash directory", e)
                    errorMsg = e.message ?: "Unknown I/O exception during trash move"
                }
            }

            // Record verification log
            if (verifySuccess) {
                addDebugLog(
                    action = "TRASH/DELETE",
                    status = "SUCCESS",
                    message = "Photo '${photo.name}' successfully moved to Trash directory"
                )
            } else {
                addDebugLog(
                    action = "TRASH/DELETE",
                    status = "FAILURE",
                    message = "Verification failed for trashing '${photo.name}': $errorMsg"
                )
            }

            val updatedPhoto = photo.copy(isDeleted = true, isSorted = true)
            repository.updatePendingPhoto(updatedPhoto)

            val history = HistoryEntity(
                photoId = photo.id,
                photoUri = photo.path,
                photoName = photo.name,
                actionType = "DELETE",
                targetAlbumId = -1,
                targetAlbumName = "Moved to Trash Bin",
                swipeDirection = "TRASH"
            )
            repository.insertHistory(history)
            _message.value = "${photo.name} moved to Trash Bin"
        }
    }

    // Recover a photo from the trash bin: cleans from physical trash directory
    fun restoreTrashedPhoto(photo: PendingPhotoEntity) {
        viewModelScope.launch {
            var verifySuccess = false
            var errorMsg: String? = null
            withContext(Dispatchers.IO) {
                try {
                    val trashDir = getTrashDir()
                    val trashFile = java.io.File(trashDir, photo.name)
                    restoreOriginalSource(trashFile, photo.path)
                    if (trashFile.exists()) {
                        trashFile.delete()
                    }
                    if (!trashFile.exists()) {
                        verifySuccess = true
                    } else {
                        errorMsg = "File still exists in Trash directory: ${trashFile.absolutePath}"
                    }
                } catch (e: Exception) {
                    Log.e("PhotoSorterViewModel", "Error removing photo from physical trash directory on restore", e)
                    errorMsg = e.message ?: "Unknown I/O exception during restore"
                }
            }

            if (verifySuccess) {
                addDebugLog(
                    action = "RESTORE",
                    status = "SUCCESS",
                    message = "Photo '${photo.name}' successfully restored from Trash"
                )
            } else {
                addDebugLog(
                    action = "RESTORE",
                    status = "FAILURE",
                    message = "Verification failed restoring '${photo.name}': $errorMsg"
                )
            }

            repository.markPhotoAsUnsorted(photo.id)
            _message.value = "Restored: ${photo.name}"
        }
    }

    // Empty the trash completely: physically deletes the files in the Trash directory from disk
    fun emptyTrash() {
        viewModelScope.launch {
            try {
                val trashed = repository.allPendingPhotos.first().filter { it.isDeleted }
                if (trashed.isNotEmpty()) {
                    var allDeleted = true
                    var errorMsg: String? = null
                    withContext(Dispatchers.IO) {
                        try {
                            val trashDir = getTrashDir()
                            trashed.forEach { photo ->
                                val f = java.io.File(trashDir, photo.name)
                                if (f.exists()) {
                                    val deleted = f.delete()
                                    if (!deleted && f.exists()) {
                                        allDeleted = false
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            allDeleted = false
                            errorMsg = e.message
                        }
                    }

                    if (allDeleted) {
                        addDebugLog(
                            action = "EMPTY_TRASH",
                            status = "SUCCESS",
                            message = "Cleared ${trashed.size} items from Trash directory on disk"
                        )
                    } else {
                        addDebugLog(
                            action = "EMPTY_TRASH",
                            status = "WARNING",
                            message = "Empty trash action complete, though some physical files could not be removed: $errorMsg"
                        )
                    }

                    repository.deletePendingPhotos(trashed)
                    _message.value = "Emptied ${trashed.size} items from Trash Bin!"
                } else {
                    _message.value = "Trash Bin is already empty!"
                    addDebugLog(
                        action = "EMPTY_TRASH",
                        status = "SUCCESS",
                        message = "Trash directory was already empty"
                    )
                }
            } catch (e: Exception) {
                Log.e("PhotoSorterViewModel", "Error emptying trash", e)
                _message.value = "Failed to empty trash"
                addDebugLog(
                    action = "EMPTY_TRASH",
                    status = "FAILURE",
                    message = "Failed to clear trash: ${e.message}"
                )
            }
        }
    }

    // Undo the last swiped action (right top corner circle or history list)
    fun undoLastSwipe() {
        viewModelScope.launch {
            val history = historyLog.value
            val lastActiveAction = history.find { !it.isUndone }

            if (lastActiveAction != null) {
                undoAction(lastActiveAction)
            } else {
                _message.value = "Nothing to undo!"
            }
        }
    }

    // Revert a specific history item, including physical files cleanup on disk
    fun undoAction(historyItem: HistoryEntity) {
        viewModelScope.launch {
            var verifySuccess = false
            var errorMsg: String? = null
            withContext(Dispatchers.IO) {
                try {
                    if (historyItem.actionType == "DELETE") {
                        // Remove from physical trash
                        val trashFile = java.io.File(getTrashDir(), historyItem.photoName)
                        restoreOriginalSource(trashFile, historyItem.photoUri)
                        if (trashFile.exists()) {
                            trashFile.delete()
                        }
                        if (!trashFile.exists()) {
                            verifySuccess = true
                        } else {
                            errorMsg = "File still exists in trash after attempt to clean up: ${trashFile.absolutePath}"
                        }
                    } else if (historyItem.actionType == "SORT") {
                        // Remove from sorted album
                        val albums = albumsFlow.value
                        val matchedAlbum = albums.find { it.id == historyItem.targetAlbumId }
                        if (matchedAlbum != null) {
                            val sortedFile = java.io.File(matchedAlbum.folderUri, historyItem.photoName)
                            restoreOriginalSource(sortedFile, historyItem.photoUri)
                            if (sortedFile.exists()) {
                                sortedFile.delete()
                            }
                            if (!sortedFile.exists()) {
                                verifySuccess = true
                            } else {
                                errorMsg = "File still exists in sorted album folder after attempt to clean up: ${sortedFile.absolutePath}"
                            }
                        } else {
                            verifySuccess = true // Album no longer exists, but file is considered gone or untracked
                        }
                    } else {
                        verifySuccess = true
                    }
                } catch (e: Exception) {
                    Log.e("PhotoSorterViewModel", "Error cleaning up disk file on undo", e)
                    errorMsg = e.message ?: "Unknown I/O exception during undo"
                }
            }

            if (verifySuccess) {
                addDebugLog(
                    action = "UNDO",
                    status = "SUCCESS",
                    message = "Successfully reverted sorting/deletion for photo '${historyItem.photoName}'"
                )
            } else {
                addDebugLog(
                    action = "UNDO",
                    status = "FAILURE",
                    message = "Verification failed on undo for '${historyItem.photoName}': $errorMsg"
                )
            }

            // Revert photo sorting states
            repository.markPhotoAsUnsorted(historyItem.photoId)

            // Mark history item as undone
            val updatedHistory = historyItem.copy(isUndone = true)
            repository.updateHistory(updatedHistory)

            _message.value = "Undid swipe for: ${historyItem.photoName}"
        }
    }
}

class PhotoSorterViewModelFactory(
    private val repository: PhotoSorterRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoSorterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoSorterViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
