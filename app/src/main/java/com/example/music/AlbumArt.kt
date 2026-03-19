package com.example.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import kotlin.random.Random

// Spotify-palette colours — matches AlbumArtPlaceholder.kt
private val PlaceholderColors = listOf(
    Color(0xFF1DB954),
    Color(0xFF2D46B9),
    Color(0xFF8D67AB),
    Color(0xFFE13300),
    Color(0xFFE8115B),
    Color(0xFF148A08),
    Color(0xFF537AA1),
    Color(0xFFAF2896),
)

@Composable
fun AlbumArt(
    uri: android.net.Uri?,
    modifier: Modifier = Modifier,
    placeholderText: String = "",
    cornerRadius: Dp = 4.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (uri != null) {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
            contentScale = contentScale
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                else -> InlinePlaceholder(
                    text = placeholderText,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = cornerRadius
                )
            }
        }
    } else {
        InlinePlaceholder(
            text = placeholderText,
            modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
            cornerRadius = cornerRadius
        )
    }
}

@Composable
private fun InlinePlaceholder(
    text: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 4.dp
) {
    val bg = PlaceholderColors[Random(text.hashCode()).nextInt(PlaceholderColors.size)]

    Box(
        modifier = modifier.background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}