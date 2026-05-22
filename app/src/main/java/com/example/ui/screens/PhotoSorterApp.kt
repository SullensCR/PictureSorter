package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import com.example.ui.theme.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.ui.components.DemoImageCanvas
import com.example.ui.components.SwipeablePhotoCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoSorterApp(viewModel: PhotoSorterViewModel) {
    val context = LocalContext.current
    val setupPhase by viewModel.setupPhase.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val albums by viewModel.albumsFlow.collectAsStateWithLifecycle()
    val activePhotos by viewModel.activePhotosFlow.collectAsStateWithLifecycle()
    val remainingCount by viewModel.remainingPhotosCount.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalPhotosCount.collectAsStateWithLifecycle()
    val historyLog by viewModel.historyLog.collectAsStateWithLifecycle()
    val trashedPhotos by viewModel.trashedPhotosFlow.collectAsStateWithLifecycle()
    val debugLogs by viewModel.debugLogs.collectAsStateWithLifecycle()

    // Handle Toast messages
    LaunchedEffect(message) {
        message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            if (setupPhase == SetupPhase.ACTIVE_SORTING) {
                NavigationBar(
                    modifier = Modifier.testTag("navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(Icons.Default.Swipe, "Workspace") },
                        label = { Text("Workspace") },
                        modifier = Modifier.testTag("workspace_tab_button")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(Icons.Default.History, "History") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("history_tab_button")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (trashedPhotos.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(trashedPhotos.size.toString(), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, "Trash")
                            }
                        },
                        label = { Text("Trash") },
                        modifier = Modifier.testTag("trash_tab_button")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        icon = { Icon(Icons.Default.Settings, "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("settings_tab_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (setupPhase) {
                SetupPhase.CHOOSE_SOURCE -> {
                    SourceSelectionScreen(
                        isLoading = isLoading,
                        onDemoClicked = { viewModel.loadDemoPhotos() },
                        onScanClicked = { viewModel.scanDevicePhotos(context) }
                    )
                }
                SetupPhase.CHOOSE_TARGETS -> {
                    TargetAlbumsScreen(
                        onSubmittingTargets = { a1, a2, a3, a4 ->
                            viewModel.setTargetAlbums(a1, a2, a3, a4)
                        },
                        onBack = { viewModel.resetAppConfiguration() }
                    )
                }
                SetupPhase.CHOOSE_SWIPES -> {
                    SwipeMappingExplanationScreen(
                        albums = albums,
                        onConfirmed = { viewModel.confirmSwipeMappings() },
                        onBack = { viewModel.resetAppConfiguration() }
                    )
                }
                SetupPhase.ACTIVE_SORTING -> {
                    when (currentTab) {
                        0 -> {
                            ActiveSortingWorkspace(
                                activePhotos = activePhotos,
                                albums = albums,
                                remainingCount = remainingCount,
                                totalCount = totalCount,
                                onSwiped = { photo, direction -> viewModel.swipePhoto(photo, direction) },
                                onDeletePermanently = { photo -> viewModel.deletePhotoPermanently(photo) },
                                onUndo = { viewModel.undoLastSwipe() },
                                onReset = { viewModel.resetAppConfiguration() },
                                trashedPhotos = trashedPhotos,
                                onRestorePhoto = { photo -> viewModel.restoreTrashedPhoto(photo) },
                                onEmptyTrash = { viewModel.emptyTrash() }
                            )
                        }
                        1 -> {
                            HistoryTab(
                                historyList = historyLog,
                                remainingCount = remainingCount,
                                totalCount = totalCount,
                                onUndoItem = { item -> viewModel.undoAction(item) }
                            )
                        }
                        2 -> {
                            TrashBinTab(
                                trashedPhotos = trashedPhotos,
                                onRestorePhoto = { photo -> viewModel.restoreTrashedPhoto(photo) },
                                onEmptyTrash = { viewModel.emptyTrash() },
                                checkFolderExists = { path -> viewModel.checkFolderExists(path) },
                                trashFolderUri = viewModel.getTrashDir().absolutePath
                            )
                        }
                        else -> {
                            SettingsTab(
                                albums = albums,
                                onClearSetup = { viewModel.resetAppConfiguration() },
                                checkFolderExists = { path -> viewModel.checkFolderExists(path) },
                                onCreateFolder = { path -> viewModel.createFolder(path) },
                                trashFolderUri = viewModel.getTrashDir().absolutePath,
                                debugLogs = debugLogs,
                                onClearLogs = { viewModel.clearDebugLogs() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================== PHASE 1: CHOOSE SOURCE ========================

@Composable
fun SourceSelectionScreen(
    isLoading: Boolean,
    onDemoClicked: () -> Unit,
    onScanClicked: () -> Unit
) {
    val context = LocalContext.current
    // Resolve right permission string based on build SDK level
    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onScanClicked()
        } else {
            Toast.makeText(context, "Gallery permission denied. Launching Sandbox Demo Mode instead!", Toast.LENGTH_LONG).show()
            onDemoClicked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("source_selection_screen"),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning device assets...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Header Icon Area
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Photo Sorter",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sort your gallery photos with intuitive, satisfying swipe gestures in seconds.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Option A: Real Gallery
                Button(
                    onClick = { launcher.launch(galleryPermission) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("scan_gallery_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Access Gallery Folder",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option B: Interactive Sandbox Demo Mode
                OutlinedButton(
                    onClick = onDemoClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("sandbox_mode_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Sandbox", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Instant Sandbox Demo Mode",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "*Demo Mode loads 12 mock masterpieces immediately so you can evaluate swipe controls inside this browser emulator.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

// ======================== PHASE 2: CHOOSE TARGETS ========================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetAlbumsScreen(
    onSubmittingTargets: (String, String, String?, String?) -> Unit,
    onBack: () -> Unit
) {
    var album1 by remember { mutableStateOf("Favorites") }
    var album2 by remember { mutableStateOf("Archive") }
    var album3 by remember { mutableStateOf("") }
    var show3rdAlbumInput by remember { mutableStateOf(false) }
    var album4 by remember { mutableStateOf("") }
    var show4thAlbumInput by remember { mutableStateOf(false) }

    val presetSuggestions = listOf("Favorites", "Archive", "Work", "Vacation", "Receipts", "Family", "Receipts", "Memories", "Trash Bin")

    // Error states
    val isAlbum1Error = album1.trim().isEmpty()
    val isAlbum2Error = album2.trim().isEmpty()
    val isFormValid = !isAlbum1Error && !isAlbum2Error && 
            (!show3rdAlbumInput || album3.trim().isNotEmpty()) &&
            (!show4thAlbumInput || album4.trim().isNotEmpty())

    // Tracks which field is active so preset chips populate it
    var activeFieldIndex by remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("target_albums_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            // Toolbar back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure Target Albums",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Define which folders/albums your images will be sorted into. Choose standard folders or type custom destinations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Text Inputs
            OutlinedTextField(
                value = album1,
                onValueChange = { album1 = it },
                label = { Text("First Target Album (LEFT Swipe)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("album_input_1"),
                isError = isAlbum1Error,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (activeFieldIndex == 1) {
                        Icon(Icons.Default.CheckCircle, "Active Field", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                supportingText = { if (isAlbum1Error) Text("This field cannot be empty") },
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = album2,
                onValueChange = { album2 = it },
                label = { Text("Second Target Album (RIGHT Swipe)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("album_input_2"),
                isError = isAlbum2Error,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (activeFieldIndex == 2) {
                        Icon(Icons.Default.CheckCircle, "Active Field", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                supportingText = { if (isAlbum2Error) Text("This field cannot be empty") },
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (show3rdAlbumInput) {
                OutlinedTextField(
                    value = album3,
                    onValueChange = { album3 = it },
                    label = { Text("Optional 3rd Album (UP Swipe)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("album_input_3"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeFieldIndex == 3) {
                                Icon(Icons.Default.CheckCircle, "Active Field", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                album3 = ""
                                show3rdAlbumInput = false
                                if (activeFieldIndex == 3) activeFieldIndex = 1
                            }) {
                                Icon(Icons.Default.Close, "Remove")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (show4thAlbumInput) {
                     OutlinedTextField(
                        value = album4,
                        onValueChange = { album4 = it },
                        label = { Text("Optional 4th Album (DOWN Swipe)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("album_input_4"),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (activeFieldIndex == 4) {
                                    Icon(Icons.Default.CheckCircle, "Active Field", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    album4 = ""
                                    show4thAlbumInput = false
                                    if (activeFieldIndex == 4) activeFieldIndex = 1
                                }) {
                                    Icon(Icons.Default.Close, "Remove")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                } else {
                    Button(
                        onClick = {
                            show4thAlbumInput = true
                            activeFieldIndex = 4
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("add_fourth_album_button")
                    ) {
                        Icon(Icons.Default.Add, "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add a 4th Album (DOWN swipe option)")
                    }
                }
            } else {
                Button(
                    onClick = {
                        show3rdAlbumInput = true
                        activeFieldIndex = 3
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .testTag("add_third_album_button")
                ) {
                    Icon(Icons.Default.Add, "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add a 3rd Album (UP swipe option)")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preset suggestions title & selector
            Text(
                text = "Tap a text field above, then tap preset chips to auto-fill quickly:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Focus selector row for tap target
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedFilterChip(
                    selected = activeFieldIndex == 1,
                    onClick = { activeFieldIndex = 1 },
                    label = { Text("Editing: Album 1") }
                )
                ElevatedFilterChip(
                    selected = activeFieldIndex == 2,
                    onClick = { activeFieldIndex = 2 },
                    label = { Text("Editing: Album 2") }
                )
                if (show3rdAlbumInput) {
                    ElevatedFilterChip(
                        selected = activeFieldIndex == 3,
                        onClick = { activeFieldIndex = 3 },
                        label = { Text("Editing: Album 3") }
                    )
                }
                if (show4thAlbumInput) {
                    ElevatedFilterChip(
                        selected = activeFieldIndex == 4,
                        onClick = { activeFieldIndex = 4 },
                        label = { Text("Editing: Album 4") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetSuggestions.forEach { preset ->
                    AssistChip(
                        onClick = {
                            when (activeFieldIndex) {
                                1 -> album1 = preset
                                2 -> album2 = preset
                                3 -> album3 = preset
                                4 -> album4 = preset
                            }
                        },
                        label = { Text(preset) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (album1 == preset || album2 == preset || album3 == preset || album4 == preset) Icons.Default.Check else Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Bottom Action Button
        Button(
            onClick = {
                if (isFormValid) {
                    onSubmittingTargets(
                        album1, 
                        album2, 
                        if (show3rdAlbumInput) album3 else null,
                        if (show4thAlbumInput) album4 else null
                    )
                }
            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .testTag("submit_targets_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Confirm Albums & Customize Swipes", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ======================== PHASE 3: CHOOSE SWIPES ========================

@Composable
fun SwipeMappingExplanationScreen(
    albums: List<AlbumEntity>,
    onConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    val upAlbum = albums.find { it.swipeDirection == "UP" }
    val leftAlbum = albums.find { it.swipeDirection == "LEFT" }
    val rightAlbum = albums.find { it.swipeDirection == "RIGHT" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("swipe_mappings_screen")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Swipe Commands",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Each swipe direction moves photos to a distinct album. Confirm the mapping below is correct before entering the active Workspace.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card 1: Left Swipe
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, "Left", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Left Swipe Gesture", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(leftAlbum?.name ?: "Album Left", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 2: Right Swipe
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, "Right", tint = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Right Swipe Gesture", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(rightAlbum?.name ?: "Album Right", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 3: Up Swipe (if configured)
            if (upAlbum != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowUpward, "Up", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Up Swipe Gesture", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text(upAlbum.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 4: Down Swipe (if configured)
            val downAlbum = albums.find { it.swipeDirection == "DOWN" }
            if (downAlbum != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, "Down", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Down Swipe Gesture", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text(downAlbum.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Delete Note Area
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, "Trash Note", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Note: Permanent delete action is always anchored to the upper-left corner for lightning-rapid clearing of actual device junk photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Bottom Start Button
        Button(
            onClick = onConfirmed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .testTag("confirm_swipes_proceed_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Begin Swipe Sorting Tasks", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ======================== PHASE 4: ACTIVE WORKSPACE ========================

@Composable
fun ActiveSortingWorkspace(
    activePhotos: List<PendingPhotoEntity>,
    albums: List<AlbumEntity>,
    remainingCount: Int,
    totalCount: Int,
    onSwiped: (PendingPhotoEntity, String) -> Unit,
    onDeletePermanently: (PendingPhotoEntity) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    trashedPhotos: List<PendingPhotoEntity>,
    onRestorePhoto: (PendingPhotoEntity) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val leftAlbumName = albums.find { it.swipeDirection == "LEFT" }?.name ?: "Family"
    val rightAlbumName = albums.find { it.swipeDirection == "RIGHT" }?.name ?: "Archive"
    val upAlbumName = albums.find { it.swipeDirection == "UP" }?.name
    val downAlbumName = albums.find { it.swipeDirection == "DOWN" }?.name

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("active_sorting_workspace")
    ) {
        // Edge Overlay Swipe Hints
        // Left side rotated label
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
                .graphicsLayer {
                    rotationZ = -90f
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Swipe Left: $leftAlbumName",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 1
            )
        }

        // Right side rotated label
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .graphicsLayer {
                    rotationZ = 90f
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Swipe Right: $rightAlbumName",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 1
            )
        }

        // Top edge overlay hint if active UP swipe album exists
        if (upAlbumName != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Swipe Up: $upAlbumName",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    maxLines = 1
                )
            }
        }

        // Bottom edge overlay hint if active DOWN swipe album exists
        if (downAlbumName != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Swipe Down: $downAlbumName",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    maxLines = 1
                )
            }
        }

        // Floating hints bottom layout circles (Left and Right helper visualizer rings)
        if (activePhotos.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 120.dp)
                    .size(48.dp)
                    .border(2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = CircleShape)
                    .alpha(0.3f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp)
                    .size(48.dp)
                    .border(2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), shape = CircleShape)
                    .alpha(0.3f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Core Layout Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Sleek Custom Top Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val topPhoto = activePhotos.firstOrNull()
                // Left circular buttons area: Move to Trash
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (topPhoto != null) {
                        IconButton(
                            onClick = { onDeletePermanently(topPhoto) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("delete_permanently_button")
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Move to Trash",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Empty spacer placeholder to align center title perfectly when stack is empty
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }

                // Center Title Column (Interactive for soft reset)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            onClick = onReset,
                            onClickLabel = "Reset config"
                        )
                ) {
                    Text(
                        text = "ORGANIZING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (leftAlbumName != "Family" || rightAlbumName != "Archive") "$leftAlbumName / $rightAlbumName" else "Active Stack",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Setup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Right circular button: Undo with White Frame style
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("undo_swipe_circle_button")
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        .border(2.dp, if (MaterialTheme.colorScheme.background == Color(0xFFF7F9FF)) Color.White else MaterialTheme.colorScheme.outline, CircleShape)
                        .shadow(1.dp, CircleShape)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo swipe action",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // B. Active Deck Zone
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (activePhotos.isNotEmpty()) {
                    val deckPhotos = activePhotos.take(3)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Sliding Card Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            deckPhotos.asReversed().forEachIndexed { reversedIdx, photo ->
                                val cardIndex = deckPhotos.size - 1 - reversedIdx
                                val isTopCard = cardIndex == 0

                                // 3D-deck style: background cards scale down and offset down slightly
                                val scale = 1f - (cardIndex * 0.04f)
                                val yOffset = (cardIndex * 12).dp

                                SwipeablePhotoCard(
                                    photo = photo,
                                    albums = albums,
                                    onSwiped = onSwiped,
                                    isInteractive = isTopCard,
                                    modifier = Modifier
                                        .fillMaxWidth(scale)
                                        .offset(y = yOffset)
                                )
                            }
                        }

                        // Subtle gesture instructions bottom hint
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val upText = if (upAlbumName != null) ", Up for $upAlbumName" else ""
                            val downText = if (downAlbumName != null) " or Down for $downAlbumName" else ""
                            Text(
                                text = "Swipe Left to $leftAlbumName, Right to $rightAlbumName$upText$downText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Empty sorting state catch-up
                    EmptyWorkspaceState(
                        onReset = onReset,
                        totalSorted = totalCount
                    )
                }
            }

            // C. Progress Area (Matching Tailwind HTML specs exactly)
            if (activePhotos.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PROGRESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${totalCount - remainingCount} / $totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = if (totalCount > 0) (totalCount - remainingCount).toFloat() / totalCount else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shadow(1.dp, RoundedCornerShape(8.dp))
                            .testTag("progress_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyWorkspaceState(
    onReset: () -> Unit,
    totalSorted: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_workspace_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Completed",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Workspace Organized!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Successfully sorted through your stack of $totalSorted photos. If you'd like to load another set or configure other target folders, click below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onReset,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RestartAlt, "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start a New Batch")
            }
        }
    }
}

// ======================== TAB 2: HISTORY LOGS & SETTINGS ========================

// ======================== SUB-TAB: HISTORY LOGS ========================

@Composable
fun HistoryTab(
    historyList: List<HistoryEntity>,
    remainingCount: Int,
    totalCount: Int,
    onUndoItem: (HistoryEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("history_tab")
    ) {
        // Stats Card Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Collection Efficiency Metrics",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total Loaded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$totalCount items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "Sorted Tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${totalCount - remainingCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "Unorganized", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$remainingCount items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Text(
            text = "Sorting Audit Log",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // List of past sorted histories
        val sortedList = remember(historyList) { historyList.filter { !it.isUndone } }

        if (sortedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlaylistRemove,
                        contentDescription = "Empty Log",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No sorted items in current log",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("history_list_column"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedList, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onUndo = { onUndoItem(item) }
                    )
                }
            }
        }
    }
}

// ======================== SUB-TAB: TRASH BIN ========================

@Composable
fun TrashBinTab(
    trashedPhotos: List<PendingPhotoEntity>,
    onRestorePhoto: (PendingPhotoEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    checkFolderExists: (String) -> Boolean,
    trashFolderUri: String
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Permanent Deletion",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to permanently delete these ${trashedPhotos.size} files? This will physically erase the files from your device storage (Trash folder) and can never be recovered."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Yes, Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("trash_bin_tab")
    ) {
        // Physical Trash Directory Representation
        val trashExists = checkFolderExists(trashFolderUri)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash Folder",
                        tint = if (trashExists) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trash Folder Directory",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = trashFolderUri,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = if (trashExists && trashedPhotos.isNotEmpty()) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface,
                        contentColor = if (trashExists && trashedPhotos.isNotEmpty()) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (trashExists && trashedPhotos.isNotEmpty()) "Active" else "Empty",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trash Bin (${trashedPhotos.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )

            if (trashedPhotos.isNotEmpty()) {
                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, "Empty All", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Empty Trash", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Text(
            text = "Items in the trash are preserved locally. You can selectively recover them or permanently wipe everything above.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (trashedPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Trash Bin is empty!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashedPhotos, key = { it.id }) { photo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photo.path.startsWith("demo://")) {
                                    DemoImageCanvas(
                                        themeUrl = photo.path,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = photo.path,
                                        contentDescription = photo.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = photo.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = photo.size,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onRestorePhoto(photo) },
                                modifier = Modifier.testTag("restore_button_${photo.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Recover photo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== SUB-TAB: SETTINGS ========================

@Composable
fun SettingsTab(
    albums: List<AlbumEntity>,
    onClearSetup: () -> Unit,
    checkFolderExists: (String) -> Boolean,
    onCreateFolder: (String) -> Unit,
    trashFolderUri: String,
    debugLogs: List<DebugLogEntry>,
    onClearLogs: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) }

    val handleCreateFolder: (String) -> Unit = { path ->
        onCreateFolder(path)
        scope.launch {
            delay(300)
            refreshTrigger++
        }
    }

    val pendingAlbums = remember(albums, refreshTrigger) { albums.filter { !checkFolderExists(it.folderUri) } }
    val trashMissing = remember(refreshTrigger) { !checkFolderExists(trashFolderUri) }
    val anyMissing = pendingAlbums.isNotEmpty() || trashMissing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_tab")
    ) {
        Text(
            text = "Active Storage Directories",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Scoped storage benefits explanation card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scoped Storage & Access Freedom",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Albums are created inside the accessible Android/media/ directory instead of Android/data/. Due to modern Google storage limitations, users cannot directly browse files in Android/data/. By writing files here, any standard file manager, photo editor, or system gallery application can easily scan, organize, and view your sorted media!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Global Create missing directories button
        if (anyMissing) {
            Button(
                onClick = {
                    pendingAlbums.forEach { onCreateFolder(it.folderUri) }
                    if (trashMissing) {
                        onCreateFolder(trashFolderUri)
                    }
                    scope.launch {
                        delay(300)
                        refreshTrigger++
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("create_missing_directories_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "Create Folders"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Missing Directories",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Albums directories
                albums.forEach { album ->
                    val exists = remember(album.folderUri, refreshTrigger) { checkFolderExists(album.folderUri) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = if (exists) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Swipe ${album.swipeDirection}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = album.folderUri,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (exists) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { handleCreateFolder(album.folderUri) },
                                    modifier = Modifier.size(32.dp).testTag("create_album_folder_${album.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = "Create folder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Trash Bin Directory representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash Folder",
                        tint = if (!trashMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Trash Bin Folder",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = trashFolderUri,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!trashMissing) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFC62828),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Pending",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { handleCreateFolder(trashFolderUri) },
                                modifier = Modifier.size(32.dp).testTag("create_trash_folder")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "Create trash folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verification & Debugging",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("debugging_logs_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debugging",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-Time I/O Verification",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (debugLogs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearLogs,
                            modifier = Modifier.testTag("clear_debug_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear logs",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear Logs",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (debugLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal placeholder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No execution logs yet.\nVerification checks run on every swipe/trash.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        debugLogs.asReversed().forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("debug_log_item_${log.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${log.timestamp}]",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = when (log.status) {
                                            "SUCCESS" -> Color(0xFFE8F5E9)
                                            "FAILURE" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFFFF3E0)
                                        },
                                        contentColor = when (log.status) {
                                            "SUCCESS" -> Color(0xFF2E7D32)
                                            "FAILURE" -> Color(0xFFC62828)
                                            else -> Color(0xFFE65100)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = log.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.action,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset config area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Resetting configuration will allow you to assign new folders, directions and clear history metrics. This does not delete any of your physical media files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = onClearSetup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, "Reset Config")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deconfigure Workspace", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: HistoryEntity,
    onUndo: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        format.format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumb preview representation
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2C))
            ) {
                if (item.photoUri.startsWith("demo://")) {
                    DemoImageCanvas(themeUrl = item.photoUri, modifier = Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = item.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.photoName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.actionType == "DELETE") {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Deleted",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Permanently Deleted",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Sorted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sent to ${item.targetAlbumName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $dateStr",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Specific Undo Button for this historical item
            OutlinedButton(
                onClick = onUndo,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("undo_button_${item.id}")
                    .minimumInteractiveComponentSize(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Undo, "revert action", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
            }
        }
    }
}


