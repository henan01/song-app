package com.songapp.ktv.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.songapp.ktv.data.QueueRow
import com.songapp.ktv.ui.components.GlassSurface
import com.songapp.ktv.ui.components.formatDuration
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.songapp.ktv.viewmodel.QueueViewModel
import java.io.File

@Composable
fun QueueScreen(
    vm: QueueViewModel = viewModel(),
    onOpenPlayer: () -> Unit
) {
    val items by vm.items.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        Header(count = items.size, onClear = { vm.clear() })
        if (items.isEmpty()) {
            Empty()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(items = items, key = { it.id }) { row ->
                    QueueRowView(
                        row = row,
                        onPlay = { vm.playItem(row); onOpenPlayer() },
                        onRemove = { vm.remove(row) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(count: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonViolet, NeonCyan))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = Color.Black) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("点歌单", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("$count 首待唱", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (count > 0) {
            TextButton(onClick = onClear) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = NeonPink)
                Spacer(Modifier.width(4.dp))
                Text("清空", color = NeonPink)
            }
        }
    }
}

@Composable
private fun QueueRowView(row: QueueRow, onPlay: () -> Unit, onRemove: () -> Unit) {
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
            val cover = row.coverPath?.let { File(it) }?.takeIf { it.exists() }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(NeonViolet.copy(alpha = .4f), NeonPink.copy(alpha = .4f)))),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) AsyncImage(model = cover, contentDescription = null, modifier = Modifier.size(48.dp))
                else Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${row.artist}  ·  ${formatDuration(row.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = NeonViolet)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = "移除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Empty() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("点歌单是空的", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("到曲库里添几首吧", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
