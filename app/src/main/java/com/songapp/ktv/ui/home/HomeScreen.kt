package com.songapp.ktv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.songapp.ktv.KtvApp
import com.songapp.ktv.ui.components.GlassSurface
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonGold
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.songapp.ktv.ui.theme.primaryGradient

@Composable
fun HomeScreen(
    onOpenLibrary: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val app = KtvApp.get()
    val count by app.repository.count().collectAsState(initial = 0)
    val queue by app.repository.queueFlow().collectAsState(initial = emptyList())
    val player by app.player.state.collectAsState()
    var roomRunning by remember { mutableStateOf(app.roomServer.isRunning) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Header()
        Spacer(Modifier.height(18.dp))
        NowCard(
            title = player.currentSong?.title ?: "还没开始唱",
            artist = player.currentSong?.artist ?: "先去歌源下载，或导入本地歌曲",
            isPlaying = player.isPlaying,
            onOpen = onOpenPlayer
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard(
                title = "开唱",
                subtitle = "$count 首歌",
                icon = Icons.Filled.PlayArrow,
                tint = NeonPink,
                modifier = Modifier.weight(1f),
                onClick = onOpenLibrary
            )
            ActionCard(
                title = "电视房间",
                subtitle = if (roomRunning) "已开启" else "投屏歌词",
                icon = Icons.Filled.LiveTv,
                tint = NeonCyan,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!app.roomServer.isRunning) app.roomServer.start()
                    roomRunning = app.roomServer.isRunning
                    onOpenSettings()
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard(
                title = "加歌源",
                subtitle = "JSON / 链接",
                icon = Icons.Filled.CloudDownload,
                tint = NeonGold,
                modifier = Modifier.weight(1f),
                onClick = onOpenSources
            )
            ActionCard(
                title = "点歌单",
                subtitle = "${queue.size} 首待唱",
                icon = Icons.Filled.QueueMusic,
                tint = NeonViolet,
                modifier = Modifier.weight(1f),
                onClick = onOpenQueue
            )
        }
        Spacer(Modifier.height(14.dp))
        GlassSurface(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
            Column(Modifier.padding(14.dp)) {
                Text("推荐流程", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("1. 在“歌源”导入 catalog.json 或添加订阅链接", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("2. 搜索并下载歌曲，音频和歌词会按同源记录保存", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3. 开启电视房间，电视看词，手机扫码点歌", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(primaryGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = Color.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("星唱 KTV", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text("家庭大屏 · 扫码点歌 · 同源歌词", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NowCard(title: String, artist: String, isPlaying: Boolean, onOpen: () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        cornerRadius = 18.dp
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(NeonPink.copy(alpha = .55f), NeonViolet.copy(alpha = .55f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isPlaying) "正在演唱" else "当前曲目", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    GlassSurface(modifier = modifier.clickable(onClick = onClick), cornerRadius = 18.dp) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
