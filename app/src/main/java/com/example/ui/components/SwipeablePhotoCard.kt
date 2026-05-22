package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AlbumEntity
import com.example.data.PendingPhotoEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePhotoCard(
    photo: PendingPhotoEntity,
    albums: List<AlbumEntity>,
    onSwiped: (PendingPhotoEntity, String) -> Unit,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Animation controller offsets, used only for end-of-gesture animation phases
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }

    // Precise live gesture tracks that update synchronously to prevent coroutine overhead / thread starvation
    var dragX by remember(photo.id) { mutableStateOf(0f) }
    var dragY by remember(photo.id) { mutableStateOf(0f) }
    var isAnimating by remember(photo.id) { mutableStateOf(false) }

    // Direct interface coordinate helpers reflecting either current animate value or user touch position
    val currentX = if (isAnimating) offsetX.value else dragX
    val currentY = if (isAnimating) offsetY.value else dragY

    // Constants for swipe detection thresholds
    val swipeThresholdX = with(density) { 140.dp.toPx() }
    val swipeThresholdY = with(density) { 140.dp.toPx() }

    // Detect if we have 3rd or 4th folder mapped
    val upAlbum = albums.find { it.swipeDirection == "UP" }
    val downAlbum = albums.find { it.swipeDirection == "DOWN" }
    val leftAlbum = albums.find { it.swipeDirection == "LEFT" }
    val rightAlbum = albums.find { it.swipeDirection == "RIGHT" }

    // Dynamic state for ongoing gesture mapping - only for the top interactive card
    val currentSwipeDirection = if (isInteractive) {
        remember(currentX, currentY) {
            val x = currentX
            val y = currentY

            when {
                // Priority is Y axis if there is an UP album and user has dragged significantly upward
                upAlbum != null && y < -swipeThresholdY && kotlin.math.abs(y) > kotlin.math.abs(x) -> "UP"
                // Priority is Y axis if there is a DOWN album and user has dragged significantly downward
                downAlbum != null && y > swipeThresholdY && kotlin.math.abs(y) > kotlin.math.abs(x) -> "DOWN"
                x < -swipeThresholdX && kotlin.math.abs(x) > kotlin.math.abs(y) -> "LEFT"
                x > swipeThresholdX && kotlin.math.abs(x) > kotlin.math.abs(y) -> "RIGHT"
                else -> "NONE"
            }
        }
    } else {
        "NONE"
    }

    // Proportional rotation angle (tilts beautifully as dragged)
    val tiltAngle = if (isInteractive) currentX / 25f else 0f

    val cardModifier = if (isInteractive) {
        modifier
            .fillMaxWidth()
            .height(420.dp)
            .graphicsLayer {
                translationX = currentX
                translationY = currentY
                rotationZ = tiltAngle
            }
            .pointerInput(photo.id) {
                detectDragGestures(
                    onDragEnd = {
                        isAnimating = true
                        coroutineScope.launch {
                            // Sync current touch coordinates prior to initiating actual Jetpack animation flow
                            offsetX.snapTo(dragX)
                            offsetY.snapTo(dragY)

                            val x = dragX
                            val y = dragY

                            when {
                                upAlbum != null && y < -swipeThresholdY && kotlin.math.abs(y) > kotlin.math.abs(x) -> {
                                    // Swipe UP successful: animate completely off-screen, then notify
                                    offsetY.animateTo(-1200f)
                                    onSwiped(photo, "UP")
                                }
                                downAlbum != null && y > swipeThresholdY && kotlin.math.abs(y) > kotlin.math.abs(x) -> {
                                    // Swipe DOWN successful: animate completely off-screen, then notify
                                    offsetY.animateTo(1200f)
                                    onSwiped(photo, "DOWN")
                                }
                                x < -swipeThresholdX && kotlin.math.abs(x) > kotlin.math.abs(y) -> {
                                    // Swipe LEFT successful: animate completely off-screen, then notify
                                    offsetX.animateTo(-1200f)
                                    onSwiped(photo, "LEFT")
                                }
                                x > swipeThresholdX && kotlin.math.abs(x) > kotlin.math.abs(y) -> {
                                    // Swipe RIGHT successful: animate completely off-screen, then notify
                                    offsetX.animateTo(1200f)
                                    onSwiped(photo, "RIGHT")
                                }
                                else -> {
                                    // Snap back to center
                                    val animX = launch { offsetX.animateTo(0f, spring()) }
                                    val animY = launch { offsetY.animateTo(0f, spring()) }
                                    animX.join()
                                    animY.join()
                                    
                                    // Reset coordinates to clear active tracking triggers
                                    dragX = 0f
                                    dragY = 0f
                                    isAnimating = false
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Cheap synchronous update that preserves perfect frame spacing with absolutely zero MainThread context-switches
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    }
                )
            }
    } else {
        modifier
            .fillMaxWidth()
            .height(420.dp)
    }

    Card(
        modifier = cardModifier.testTag("photo_item_card_${photo.id}"),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(4.dp, if (MaterialTheme.colorScheme.background == Color(0xFFF7F9FF)) Color.White else MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInteractive) 16.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Core Image View (Either local demo Canvas or AsyncImage)
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

            // 2. Translucent Bottom Info Vignette Accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xEE000000)),
                            startY = 0f
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        text = photo.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = photo.size.ifEmpty { "Dimensions matches container" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Light
                        )
                    )
                }
            }

            // 3. Dynamic Interactive Feedback Overlays
            when (currentSwipeDirection) {
                "LEFT" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB31E3C72)) // Elegant dark indigo overlay
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SwipeLeft,
                                contentDescription = "Swipe Left",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "MOVE TO:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = leftAlbum?.name ?: "Left Album",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                "RIGHT" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB32E7D32)) // Bright Material green overlay
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SwipeRight,
                                contentDescription = "Swipe Right",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "MOVE TO:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = rightAlbum?.name ?: "Right Album",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                "UP" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB3D81B60)) // Vivid pink overlay
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SwipeUp,
                                contentDescription = "Swipe Up",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "MOVE TO:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = upAlbum?.name ?: "Up Album",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                "DOWN" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB3E65100)) // Deep amber/orange overlay
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDownward,
                                contentDescription = "Swipe Down",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "MOVE TO:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = downAlbum?.name ?: "Down Album",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
