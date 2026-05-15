package com.songapp.ktv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.songapp.ktv.data.SeparationStatus
import com.songapp.ktv.data.Song
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import java.io.File
import java.util.Locale

@Composable
fun SongRow(
    song: Song,
    onPlay: () -> Unit,
    onEnqueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    isCurrentlyPlaying: Boolean = false,
    onLong: () -> Unit = {}
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onPlay),
        cornerRadius = 18.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Cover(song)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentlyPlaying) {
                        Icon(
                            Icons.Filled.Equalizer,
                            contentDescription = "正在播放",
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrentlyPlaying) NeonPink else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(song.separationStatus)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${song.artist}  ·  ${formatDuration(song.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (song.isFavorite) NeonPink else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEnqueue) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = NeonCyan)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = NeonViolet)
            }
        }
    }
}

@Composable
private fun Cover(song: Song) {
    val cover = song.coverPath?.let { File(it) }?.takeIf { it.exists() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(NeonViolet.copy(alpha = 0.4f), NeonPink.copy(alpha = 0.4f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cover != null) {
            AsyncImage(model = cover, contentDescription = null, modifier = Modifier.size(48.dp))
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun StatusBadge(status: SeparationStatus) {
    val (text, color) = when (status) {
        SeparationStatus.DONE -> "已分离" to NeonCyan
        SeparationStatus.RUNNING -> "分离中" to NeonGoldDimmed
        SeparationStatus.PENDING -> "待处理" to MaterialTheme.colorScheme.onSurfaceVariant
        SeparationStatus.FAILED -> "失败" to NeonPink
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

private val NeonGoldDimmed = Color(0xFFFFD58E)

internal fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val s = ms / 1000
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}
