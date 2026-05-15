package com.songapp.ktv.ui.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.songapp.ktv.ui.components.LyricView
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonGold
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.songapp.ktv.ui.theme.primaryGradient
import com.songapp.ktv.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun PlayerScreen(
    vm: PlayerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val fetching by vm.fetchingLyrics.collectAsState()
    val statusMsg by vm.lyricsStatus.collectAsState()
    val toast by vm.toast.collectAsState()
    val song = state.currentSong

    var lyricsFocus by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(lyrics.isEmpty()) {
        // 没有歌词时把封面亮出来，有歌词时默认走歌词模式
        lyricsFocus = lyrics.isNotEmpty()
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(toast) {
        toast?.let {
            scope.launch { snackbar.showSnackbar(it) }
            vm.consumeToast()
        }
    }

    val pickLrc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importLyricsFromUri(it) } }

    val coverHeight by animateDpAsState(
        targetValue = if (lyricsFocus) 120.dp else 220.dp,
        animationSpec = tween(280),
        label = "coverHeight"
    )
    val coverDiscSize by animateDpAsState(
        targetValue = if (lyricsFocus) 100.dp else 220.dp,
        animationSpec = tween(280),
        label = "discSize"
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            TopBar(
                onBack = onBack,
                fetching = fetching,
                onFetchLyrics = { vm.fetchLyricsOnline() },
                onImportLyrics = { pickLrc.launch(arrayOf("application/octet-stream", "text/*", "*/*")) }
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .clickable { lyricsFocus = !lyricsFocus },
                contentAlignment = Alignment.Center
            ) {
                CoverDisc(coverPath = song?.coverPath, isPlaying = state.isPlaying, discSize = coverDiscSize)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = song?.title ?: "未播放",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            )
            Text(
                text = song?.artist ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { lyricsFocus = !lyricsFocus }
            ) {
                LyricView(
                    lines = lyrics,
                    positionMs = state.positionMs,
                    fetching = fetching,
                    statusMessage = statusMsg,
                    onFetchOnline = { vm.fetchLyricsOnline() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            VocalSlider(
                value = state.vocalLevel,
                onChange = vm::setVocalLevel
            )

            ProgressBar(
                position = state.positionMs,
                duration = state.durationMs,
                onSeek = vm::seek
            )

            Controls(
                isPlaying = state.isPlaying,
                onPrev = vm::prev,
                onTogglePlay = vm::togglePlay,
                onNext = vm::next
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    fetching: Boolean,
    onFetchLyrics: () -> Unit,
    onImportLyrics: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "正在演唱  ·  点封面/歌词区切换",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onImportLyrics) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = "导入歌词", tint = MaterialTheme.colorScheme.tertiary)
        }
        IconButton(onClick = onFetchLyrics) {
            if (fetching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = "在线搜索歌词",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        IconButton(onClick = { /* future: open queue sheet */ }) {
            Icon(Icons.Filled.QueueMusic, contentDescription = "队列", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CoverDisc(coverPath: String?, isPlaying: Boolean, discSize: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )
    val rotation = if (isPlaying) angle else 0f
    Box(
        modifier = Modifier
            .size(discSize)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(NeonPink.copy(alpha = .35f), NeonViolet.copy(alpha = .15f), Color.Transparent))),
        contentAlignment = Alignment.Center
    ) {
        val inner = discSize - 30.dp
        Box(
            modifier = Modifier
                .size(inner)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonViolet, NeonPink, NeonCyan)))
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            val cover = coverPath?.let { File(it) }?.takeIf { it.exists() }
            val photoSize = inner - 14.dp
            Box(
                modifier = Modifier.size(photoSize).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    AsyncImage(model = cover, contentDescription = null, modifier = Modifier.size(photoSize).clip(CircleShape))
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(photoSize / 3))
                }
            }
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Black)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun VocalSlider(value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MicOff, contentDescription = null, tint = if (value < 0.05f) NeonGold else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                if (value < 0.05f) "伴唱模式" else if (value > 0.95f) "原唱模式" else "混合 ${(value * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = NeonGold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = if (value > 0.95f) NeonGold else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = NeonPink,
                activeTrackColor = NeonPink,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
        Text(
            "← 关闭人声 (伴唱)        放大人声 (原唱) →",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProgressBar(position: Long, duration: Long, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val safeDuration = duration.coerceAtLeast(1L)
    val v = if (dragging) dragValue else (position.toFloat() / safeDuration).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Slider(
            value = v,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                dragging = false
                onSeek((dragValue * safeDuration).toLong())
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.10f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Controls(isPlaying: Boolean, onPrev: () -> Unit, onTogglePlay: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.width(20.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(primaryGradient()),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.Black,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(Modifier.width(20.dp))
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "下一首", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}
