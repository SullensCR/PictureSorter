package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DemoImageCanvas(themeUrl: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2C))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (themeUrl) {
                "demo://mount_fuji" -> drawMountFuji(w, h)
                "demo://bora_bora" -> drawBoraBora(w, h)
                "demo://neon_tokyo" -> drawNeonTokyo(w, h)
                "demo://desert_dunes" -> drawDesertDunes(w, h)
                "demo://nordic_cabin" -> drawNordicCabin(w, h)
                "demo://misty_bridge" -> drawMistyBridge(w, h)
                "demo://emerald_lake" -> drawEmeraldLake(w, h)
                "demo://retro_cafe" -> drawRetroCafe(w, h)
                "demo://cosmic_aurora" -> drawCosmicAurora(w, h)
                "demo://autumn_park" -> drawAutumnPark(w, h)
                "demo://cute_kitten" -> drawCuteKitten(w, h)
                "demo://vintage_car" -> drawVintageCar(w, h)
                else -> drawFallback(w, h)
            }
        }
    }
}

private fun DrawScope.drawMountFuji(w: Float, h: Float) {
    // Elegant deep purple sky grad
    val skyGrad = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F0F1A), Color(0xFF3B1E48), Color(0xFF6B2D5C)),
        startY = 0f,
        endY = h
    )
    drawRect(brush = skyGrad, size = Size(w, h))

    // Glowing vermillion sun
    drawCircle(
        color = Color(0xFFFF4E50),
        radius = h * 0.18f,
        center = Offset(w * 0.5f, h * 0.45f)
    )

    // Mountain path
    val leftBase = Offset(w * -0.1f, h * 0.95f)
    val rightBase = Offset(w * 1.1f, h * 0.95f)
    val peak = Offset(w * 0.5f, h * 0.38f)

    val mountainPath = Path().apply {
        moveTo(leftBase.x, leftBase.y)
        lineTo(peak.x, peak.y)
        lineTo(rightBase.x, rightBase.y)
        close()
    }
    drawPath(path = mountainPath, color = Color(0xFF1E1428))

    // Snowy Cap
    val snowPeakLeft = Offset(w * 0.41f, h * 0.49f)
    val snowPeakRight = Offset(w * 0.59f, h * 0.49f)
    val snowPath = Path().apply {
        moveTo(peak.x, peak.y)
        lineTo(snowPeakLeft.x, snowPeakLeft.y)
        quadraticTo(w * 0.5f, h * 0.54f, snowPeakRight.x, snowPeakRight.y)
        close()
    }
    drawPath(path = snowPath, color = Color(0xFFFFFFFF))
}

private fun DrawScope.drawBoraBora(w: Float, h: Float) {
    // Sunny sky
    val sky = Brush.verticalGradient(
        colors = listOf(Color(0xFF6DD5FA), Color(0xFFFFFFFF)),
        startY = 0f,
        endY = h * 0.6f
    )
    drawRect(brush = sky, size = Size(w, h))

    // Golden sun
    drawCircle(
        color = Color(0xFFFDC830),
        radius = h * 0.09f,
        center = Offset(w * 0.8f, h * 0.18f)
    )

    // Turquoise Ocean gradient
    val ocean = Brush.verticalGradient(
        colors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
        startY = h * 0.5f,
        endY = h
    )
    drawRect(
        brush = ocean,
        topLeft = Offset(0f, h * 0.55f),
        size = Size(w, h * 0.45f)
    )

    // Sandy island
    val island = Path().apply {
        moveTo(w * -0.1f, h)
        quadraticTo(w * 0.3f, h * 0.75f, w * 0.6f, h)
        close()
    }
    drawPath(path = island, color = Color(0xFFF7E290))

    // Palm Tree trunk
    val trunk = Path().apply {
        moveTo(w * 0.15f, h * 0.88f)
        quadraticTo(w * 0.22f, h * 0.7f, w * 0.28f, h * 0.6f)
        lineTo(w * 0.32f, h * 0.61f)
        quadraticTo(w * 0.26f, h * 0.72f, w * 0.2f, h * 0.89f)
        close()
    }
    drawPath(path = trunk, color = Color(0xFF6D4C41))

    // Palm leaves
    drawCircle(color = Color(0xFF2E7D32), radius = h * 0.05f, center = Offset(w * 0.28f, h * 0.58f))
    drawCircle(color = Color(0xFF4CAF50), radius = h * 0.04f, center = Offset(w * 0.35f, h * 0.56f))
    drawCircle(color = Color(0xFF1B5E20), radius = h * 0.045f, center = Offset(w * 0.22f, h * 0.59f))
}

private fun DrawScope.drawNeonTokyo(w: Float, h: Float) {
    // Cyberpunk dark background
    drawRect(color = Color(0xFF0A0214))

    // Grid lines perspective
    val gridPaint = Paint().apply {
        color = Color(0xFF3F0A5F)
        strokeWidth = 2f
        style = PaintingStyle.Stroke
    }
    val gridYOffset = h * 0.6f
    for (i in 0..10) {
        val xVal = w * (i / 10f)
        drawLine(
            color = Color(0x6A9C27B0),
            start = Offset(xVal, h),
            end = Offset(w * 0.5f, gridYOffset),
            strokeWidth = 3f
        )
    }

    // Glowing retro sun outline
    drawCircle(
        color = Color(0xFFFF007F),
        radius = h * 0.12f,
        center = Offset(w * 0.5f, gridYOffset - h * 0.1f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)
    )

    // Skyscrapers silhouettes and neon signs
    drawRect(
        color = Color(0xFF13052A),
        topLeft = Offset(w * 0.1f, h * 0.4f),
        size = Size(w * 0.22f, h * 0.35f)
    )
    drawRect(
        color = Color(0xFF160633),
        topLeft = Offset(w * 0.65f, h * 0.3f),
        size = Size(w * 0.25f, h * 0.45f)
    )

    // Vertical violet neon beams
    drawLine(
        color = Color(0xFF00FFFF),
        start = Offset(w * 0.15f, h * 0.42f),
        end = Offset(w * 0.15f, h * 0.72f),
        strokeWidth = 6f
    )
    drawLine(
        color = Color(0xFFFF007F),
        start = Offset(w * 0.75f, h * 0.33f),
        end = Offset(w * 0.75f, h * 0.68f),
        strokeWidth = 8f
    )
}

private fun DrawScope.drawDesertDunes(w: Float, h: Float) {
    // Warm sky gradient
    val sky = Brush.verticalGradient(
        colors = listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFCC80)),
        startY = 0f,
        endY = h * 0.5f
    )
    drawRect(brush = sky, size = Size(w, h))

    // Distant soft Sun
    drawCircle(
        color = Color(0xFFFFF3E0),
        radius = h * 0.08f,
        center = Offset(w * 0.3f, h * 0.25f)
    )

    // Sinuous golden sand dunes
    val dune1 = Path().apply {
        moveTo(0f, h * 0.55f)
        quadraticTo(w * 0.4f, h * 0.5f, w, h * 0.65f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path = dune1, color = Color(0xFFE5A93B))

    val dune2 = Path().apply {
        moveTo(w, h * 0.68f)
        quadraticTo(w * 0.6f, h * 0.75f, 0f, h * 0.72f)
        lineTo(0f, h)
        lineTo(w, h)
        close()
    }
    drawPath(path = dune2, color = Color(0xFFC78328))

    val dune3 = Path().apply {
        moveTo(0f, h * 0.82f)
        quadraticTo(w * 0.5f, h * 0.78f, w, h * 0.88f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path = dune3, color = Color(0xFFA5631A))
}

private fun DrawScope.drawNordicCabin(w: Float, h: Float) {
    // Cold dusk sky
    val sky = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1), Color(0xFF263238)),
        startY = 0f,
        endY = h
    )
    drawRect(brush = sky, size = Size(w, h))

    // Tiny stars
    drawCircle(color = Color.White, radius = 2f, center = Offset(w * 0.2f, h * 0.15f))
    drawCircle(color = Color.White, radius = 2f, center = Offset(w * 0.45f, h * 0.12f))
    drawCircle(color = Color.White, radius = 3f, center = Offset(w * 0.85f, h * 0.22f))

    // Pine trees silhouettes
    val treePath = Path().apply {
        moveTo(w * 0.15f, h * 0.45f)
        lineTo(w * 0.05f, h * 0.75f)
        lineTo(w * 0.25f, h * 0.75f)
        close()
    }
    drawPath(path = treePath, color = Color(0xFF0F1E19))

    // Cozy Cabin
    val cabinRect = Rect(w * 0.4f, h * 0.55f, w * 0.75f, h * 0.78f)
    drawRoundRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(cabinRect.left, cabinRect.top),
        size = Size(cabinRect.width, cabinRect.height),
        cornerRadius = CornerRadius(10f)
    )

    // Triangular Roof
    val roofPath = Path().apply {
        moveTo(w * 0.35f, h * 0.55f)
        lineTo(w * 0.575f, h * 0.42f)
        lineTo(w * 0.8f, h * 0.55f)
        close()
    }
    drawPath(path = roofPath, color = Color(0xFF2D1E18))

    // Warm glowing window
    drawRoundRect(
        color = Color(0xFFFFEB3B),
        topLeft = Offset(w * 0.48f, h * 0.62f),
        size = Size(w * 0.1f, h * 0.07f),
        cornerRadius = CornerRadius(5f)
    )

    // Snow cover ground
    drawRoundRect(
        color = Color(0xFFECF0F1),
        topLeft = Offset(-10f, h * 0.76f),
        size = Size(w + 20f, h * 0.3f),
        cornerRadius = CornerRadius(15f, 15f)
    )
}

private fun DrawScope.drawMistyBridge(w: Float, h: Float) {
    // Pale grey-blue backdrop
    drawRect(color = Color(0xFFCFD8DC))

    // Bridge structural cables in red
    val bridgeRed = Color(0xFFE74C3C)

    // Left tower peeking
    drawRect(
        color = bridgeRed,
        topLeft = Offset(w * 0.25f, h * 0.2f),
        size = Size(w * 0.08f, h * 0.6f)
    )
    // Right tower peeking
    drawRect(
        color = bridgeRed,
        topLeft = Offset(w * 0.65f, h * 0.15f),
        size = Size(w * 0.08f, h * 0.65f)
    )

    // Suspension bridge arcs
    val roadY = h * 0.6f
    drawLine(
        color = bridgeRed,
        start = Offset(0f, h * 0.35f),
        end = Offset(w * 0.29f, h * 0.45f),
        strokeWidth = 4f
    )
    drawLine(
        color = bridgeRed,
        start = Offset(w * 0.29f, h * 0.45f),
        end = Offset(w * 0.69f, h * 0.38f),
        strokeWidth = 4f
    )
    drawLine(
        color = bridgeRed,
        start = Offset(0f, roadY),
        end = Offset(w, roadY),
        strokeWidth = 8f
    )

    // Misty foggy cloud overlay
    drawCircle(color = Color(0x99ECEFF1), radius = h * 0.14f, center = Offset(w * 0.5f, h * 0.65f))
    drawCircle(color = Color(0xB3FFFFFF), radius = h * 0.18f, center = Offset(w * 0.2f, h * 0.7f))
    drawCircle(color = Color(0xB3FFFFFF), radius = h * 0.2f, center = Offset(w * 0.8f, h * 0.72f))
}

private fun DrawScope.drawEmeraldLake(w: Float, h: Float) {
    // Pine green sky reflection gradient
    val mirror = Brush.verticalGradient(
        colors = listOf(Color(0xFF145A32), Color(0xFF1E8449), Color(0xFF27AE60)),
        startY = 0f,
        endY = h
    )
    drawRect(brush = mirror, size = Size(w, h))

    // Draw dark towering pine tree triangles
    val base = h * 0.55f
    val tree1 = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.35f, base)
        lineTo(w * 0.65f, base)
        close()
    }
    drawPath(path = tree1, color = Color(0xFF0B3C1D))

    val tree2 = Path().apply {
        moveTo(w * 0.25f, h * 0.22f)
        lineTo(w * 0.12f, base)
        lineTo(w * 0.38f, base)
        close()
    }
    drawPath(path = tree2, color = Color(0xFF0F4E26))

    // Distinct glassy reflection boundary
    drawLine(
        color = Color(0x76FFFFFF),
        start = Offset(0f, base),
        end = Offset(w, base),
        strokeWidth = 4f
    )

    // Water ripple rings
    drawOval(
        color = Color(0x3BFFFFFF),
        topLeft = Offset(w * 0.33f, base + h * 0.05f),
        size = Size(w * 0.34f, h * 0.04f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
}

private fun DrawScope.drawRetroCafe(w: Float, h: Float) {
    // Warm wood table color
    drawRect(color = Color(0xFFD35400))

    // Vintage Record center label style
    drawCircle(
        color = Color(0xFF111111),
        radius = h * 0.28f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Grooves inside vinyl
    drawCircle(
        color = Color(0xFF333333),
        radius = h * 0.22f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
    drawCircle(
        color = Color(0xFF333333),
        radius = h * 0.16f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    // Colored record center
    drawCircle(
        color = Color(0xFFF1C40F),
        radius = h * 0.08f,
        center = Offset(w * 0.5f, h * 0.5f)
    )
    // Center spindle hole
    drawCircle(
        color = Color(0xFFD35400),
        radius = h * 0.02f,
        center = Offset(w * 0.5f, h * 0.5f)
    )
}

private fun DrawScope.drawCosmicAurora(w: Float, h: Float) {
    // Deep space
    drawRect(color = Color(0xFF01010D))

    // Distant galaxy nebula glow
    val nebula = Brush.radialGradient(
        colors = listOf(Color(0xFF3A0077), Color(0x00000000)),
        center = Offset(w * 0.3f, h * 0.4f),
        radius = w * 0.6f
    )
    drawCircle(
        brush = nebula,
        radius = w * 0.6f,
        center = Offset(w * 0.3f, h * 0.4f)
    )

    // Wave style neon-green Aurora trails
    val auroraPath1 = Path().apply {
        moveTo(0f, h * 0.15f)
        cubicTo(w * 0.25f, h * 0.35f, w * 0.65f, h * 0.05f, w, h * 0.25f)
        lineTo(w, h * 0.32f)
        cubicTo(w * 0.65f, h * 0.12f, w * 0.25f, h * 0.42f, 0f, h * 0.22f)
        close()
    }
    drawPath(
        path = auroraPath1,
        color = Color(0xFF4EFE3F),
        alpha = 0.5f
    )

    val auroraPath2 = Path().apply {
        moveTo(0f, h * 0.3f)
        cubicTo(w * 0.35f, h * 0.18f, w * 0.7f, h * 0.5f, w, h * 0.38f)
        lineTo(w, h * 0.44f)
        cubicTo(w * 0.7f, h * 0.56f, w * 0.35f, h * 0.24f, 0f, h * 0.36f)
        close()
    }
    drawPath(
        path = auroraPath2,
        color = Color(0xFF00E5FF),
        alpha = 0.41f
    )
}

private fun DrawScope.drawAutumnPark(w: Float, h: Float) {
    // Soft morning fog gradient
    val sky = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFCC80), Color(0xFFE3F2FD)),
        startY = 0f,
        endY = h * 0.6f
    )
    drawRect(brush = sky, size = Size(w, h))

    // Scenic walking path trailing off
    val path = Path().apply {
        moveTo(w * 0.45f, h * 0.6f)
        lineTo(w * 0.55f, h * 0.6f)
        lineTo(w * 0.85f, h)
        lineTo(w * 0.15f, h)
        close()
    }
    drawPath(path = path, color = Color(0xFFB0BEC5))

    // Whimsical orange foliage blobs
    drawCircle(color = Color(0xFFE67E22), radius = h * 0.08f, center = Offset(w * 0.2f, h * 0.48f))
    drawCircle(color = Color(0xFFD35400), radius = h * 0.06f, center = Offset(w * 0.14f, h * 0.52f))
    drawCircle(color = Color(0xFFF1C40F), radius = h * 0.07f, center = Offset(w * 0.28f, h * 0.5f))

    drawCircle(color = Color(0xFFE67E22), radius = h * 0.09f, center = Offset(w * 0.82f, h * 0.45f))
    drawCircle(color = Color(0xFFF39C12), radius = h * 0.06f, center = Offset(w * 0.72f, h * 0.5f))
}

private fun DrawScope.drawCuteKitten(w: Float, h: Float) {
    val magentaBg = Brush.verticalGradient(
        colors = listOf(Color(0xFFEC407A), Color(0xFFF48FB1)),
        startY = 0f,
        endY = h
    )
    drawRect(brush = magentaBg, size = Size(w, h))

    val kCenter = Offset(w * 0.5f, h * 0.61f)
    val catRadius = h * 0.16f

    // Ears
    val leftEar = Path().apply {
        moveTo(kCenter.x - catRadius * 0.8f, kCenter.y - catRadius * 0.5f)
        lineTo(kCenter.x - catRadius * 0.95f, kCenter.y - catRadius * 1.3f)
        lineTo(kCenter.x - catRadius * 0.2f, kCenter.y - catRadius * 0.9f)
        close()
    }
    drawPath(path = leftEar, color = Color(0xFFECEFF1))

    val rightEar = Path().apply {
        moveTo(kCenter.x + catRadius * 0.8f, kCenter.y - catRadius * 0.5f)
        lineTo(kCenter.x + catRadius * 0.95f, kCenter.y - catRadius * 1.3f)
        lineTo(kCenter.x + catRadius * 0.2f, kCenter.y - catRadius * 0.9f)
        close()
    }
    drawPath(path = rightEar, color = Color(0xFFECEFF1))

    // Face
    drawCircle(color = Color(0xFFECEFF1), radius = catRadius, center = kCenter)

    // Cheek blush
    drawCircle(color = Color(0xFFFF8A80), radius = catRadius * 0.2f, center = Offset(kCenter.x - catRadius * 0.6f, kCenter.y + catRadius * 0.15f))
    drawCircle(color = Color(0xFFFF8A80), radius = catRadius * 0.2f, center = Offset(kCenter.x + catRadius * 0.6f, kCenter.y + catRadius * 0.15f))

    // Eyes
    drawCircle(color = Color(0xFF2E7D32), radius = catRadius * 0.18f, center = Offset(kCenter.x - catRadius * 0.35f, kCenter.y - catRadius * 0.15f))
    drawCircle(color = Color(0xFF2E7D32), radius = catRadius * 0.18f, center = Offset(kCenter.x + catRadius * 0.35f, kCenter.y - catRadius * 0.15f))

    drawCircle(color = Color.Black, radius = catRadius * 0.12f, center = Offset(kCenter.x - catRadius * 0.35f, kCenter.y - catRadius * 0.15f))
    drawCircle(color = Color.Black, radius = catRadius * 0.12f, center = Offset(kCenter.x + catRadius * 0.35f, kCenter.y - catRadius * 0.15f))

    // Nose
    val nose = Path().apply {
        moveTo(kCenter.x, kCenter.y + catRadius * 0.05f)
        lineTo(kCenter.x - 12f, kCenter.y - 4f)
        lineTo(kCenter.x + 12f, kCenter.y - 4f)
        close()
    }
    drawPath(path = nose, color = Color(0xFFFF4081))
}

private fun DrawScope.drawVintageCar(w: Float, h: Float) {
    val retroSunset = Brush.verticalGradient(
        colors = listOf(Color(0xFF212121), Color(0xFF424242), Color(0xFFE65100)),
        startY = 0f,
        endY = h
    )
    drawRect(brush = retroSunset, size = Size(w, h))

    // Classic car body
    val bodyColor = Color(0xFFC0392B)
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(w * 0.2f, h * 0.5f),
        size = Size(w * 0.6f, h * 0.12f),
        cornerRadius = CornerRadius(15f)
    )

    // Cabin Coupe
    val cabin = Path().apply {
        moveTo(w * 0.35f, h * 0.51f)
        lineTo(w * 0.42f, h * 0.43f)
        lineTo(w * 0.62f, h * 0.43f)
        lineTo(w * 0.68f, h * 0.51f)
        close()
    }
    drawPath(path = cabin, color = Color(0x90FFFFFF))

    // Wheels
    drawCircle(color = Color(0xFF111111), radius = h * 0.05f, center = Offset(w * 0.33f, h * 0.63f))
    drawCircle(color = Color(0xFF111111), radius = h * 0.05f, center = Offset(w * 0.67f, h * 0.63f))

    drawCircle(color = Color(0xFFBDC3C7), radius = h * 0.02f, center = Offset(w * 0.33f, h * 0.63f))
    drawCircle(color = Color(0xFFBDC3C7), radius = h * 0.02f, center = Offset(w * 0.67f, h * 0.63f))
}

private fun DrawScope.drawFallback(w: Float, h: Float) {
    val grad = Brush.sweepGradient(
        colors = listOf(Color(0xFF8E44AD), Color(0xFF3498DB), Color(0xFF2ECC71), Color(0xFFF1C40F), Color(0xFFE74C3C), Color(0xFF8E44AD)),
        center = Offset(w * 0.5f, h * 0.5f)
    )
    drawCircle(brush = grad, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
}
