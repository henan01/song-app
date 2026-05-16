package com.songapp.ktv.ui.online

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.songapp.ktv.network.TrackMeta
import com.songapp.ktv.ui.components.GlassSurface
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonGold
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.songapp.ktv.ui.theme.primaryGradient
import com.songapp.ktv.viewmodel.OnlineSearchViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OnlineSearchScreen(
    vm: OnlineSearchViewModel = viewModel(),
    onOpenPlayer: () -> Unit
) {
    val query by vm.query.collectAsState()
    val searching by vm.searching.collectAsState()
    val results by vm.results.collectAsState()
    val downloading by vm.downloading.collectAsState()
    val downloaded by vm.downloaded.collectAsState()
    val progress by vm.progress.collectAsState()
    val toast by vm.toast.collectAsState()
    val enabledSources by vm.enabledSources.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(toast) {
        toast?.let {
            scope.launch { snackbar.showSnackbar(it) }
            vm.consumeToast()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            Header()
            Spacer(Modifier.height(6.dp))
            SearchInput(
                query = query,
                onQuery = vm::setQuery,
                onSubmit = { vm.search() },
                searching = searching
            )
            Spacer(Modifier.height(6.dp))
            SourceChips(
                allSources = vm.allSources,
                enabled = enabledSources,
                onToggle = vm::toggleSource
            )
            Hint()
            if (results.isEmpty() && !searching) {
                EmptyHint()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    items(items = results, key = { "${it.sourceId}_${it.trackId}" }) { track ->
                        val localId = "${track.sourceId}_${track.trackId}"
                        ResultRow(
                            track = track,
                            isDownloading = localId in downloading,
                            isDownloaded = localId in downloaded,
                            progressRatio = progress[localId] ?: 0f,
                            onDownload = { vm.download(track) },
                            onPlay = { vm.playDownloaded(track); onOpenPlayer() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(primaryGradient()),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Public, contentDescription = null, tint = Color.Black) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("在线搜歌", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "多音乐源 · 仅支持免费曲目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit,
    searching: Boolean
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 28.dp
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("输入歌名 / 歌手 / 关键字，回车搜索") },
            leadingIcon = {
                if (searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = NeonPink
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SourceChips(
    allSources: List<com.songapp.ktv.network.MusicSource>,
    enabled: Set<String>,
    onToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "音乐源",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        allSources.forEach { src ->
            FilterChip(
                selected = src.id in enabled,
                onClick = { onToggle(src.id) },
                label = { Text(src.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonViolet.copy(alpha = 0.22f),
                    selectedLabelColor = NeonViolet,
                    containerColor = Color.White.copy(alpha = 0.04f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun Hint() {
    Text(
        "下载会保存到本设备：MP3 + 封面 + 歌词。VIP / 无版权曲目会标记不可下载。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Public, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("搜你想唱的歌", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "默认同时搜「网易云」和「酷我」，结果合并显示",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultRow(
    track: TrackMeta,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    progressRatio: Float,
    onDownload: () -> Unit,
    onPlay: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(enabled = isDownloaded, onClick = onPlay),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonViolet.copy(alpha = .4f), NeonPink.copy(alpha = .4f)))),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.coverUrl.isNullOrBlank()) {
                        AsyncImage(model = track.coverUrl, contentDescription = null, modifier = Modifier.size(48.dp))
                    } else {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SourceBadge(sourceId = track.sourceId)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${track.artist}  ·  ${formatDuration(track.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    isDownloaded -> {
                        IconButton(onClick = onPlay) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = NeonViolet)
                        }
                        Icon(Icons.Filled.CheckCircle, contentDescription = "已下载", tint = NeonCyan)
                    }
                    isDownloading -> {
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { if (progressRatio > 0f) progressRatio else 0.05f },
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = NeonGold
                            )
                            Text("${(progressRatio * 100).toInt()}", style = MaterialTheme.typography.labelSmall, color = NeonGold)
                        }
                    }
                    else -> {
                        IconButton(onClick = onDownload) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = "下载", tint = NeonPink)
                        }
                    }
                }
            }
            if (isDownloading) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (progressRatio > 0f) progressRatio else 0.05f },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonGold,
                    trackColor = Color.White.copy(alpha = 0.10f)
                )
            }
        }
    }
}

@Composable
private fun SourceBadge(sourceId: String) {
    val (label, color) = when (sourceId) {
        "netease" -> "网易" to NeonPink
        "kuwo" -> "酷我" to NeonCyan
        else -> sourceId to NeonViolet
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val s = ms / 1000
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}
