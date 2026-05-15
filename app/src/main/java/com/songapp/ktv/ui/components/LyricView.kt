package com.songapp.ktv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songapp.ktv.lyrics.LrcLine
import com.songapp.ktv.lyrics.LrcParser
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonGold

@Composable
fun LyricView(
    lines: List<LrcLine>,
    positionMs: Long,
    fetching: Boolean = false,
    statusMessage: String? = null,
    onFetchOnline: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (lines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        fetching -> "正在搜索歌词…"
                        statusMessage != null -> statusMessage
                        else -> "暂无歌词"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                if (onFetchOnline != null) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onFetchOnline,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan.copy(alpha = 0.22f),
                            contentColor = NeonCyan
                        )
                    ) {
                        if (fetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan
                            )
                        } else {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(if (fetching) "搜索中…" else "在线搜索歌词")
                    }
                }
            }
        }
        return
    }
    val active = LrcParser.activeLineIndex(lines, positionMs)
    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 当前行落在视图上 1/3 处（剩余 2/3 留给待唱）
        val topPadding = maxHeight * 0.32f
        val bottomPadding = maxHeight * 0.42f

        LaunchedEffect(active) {
            if (active >= 0) {
                listState.animateScrollToItem(active, 0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = bottomPadding,
                start = 24.dp,
                end = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(items = lines, key = { it.timeMs }) { line ->
                val isActive = lines.indexOf(line) == active
                val alpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.55f,
                    animationSpec = tween(220),
                    label = "alpha"
                )
                val fontSize = if (isActive) 26.sp else 18.sp
                val weight = if (isActive) FontWeight.Bold else FontWeight.Medium
                val color = if (isActive) NeonGold else MaterialTheme.colorScheme.onSurface
                Text(
                    text = line.text,
                    color = color,
                    fontSize = fontSize,
                    fontWeight = weight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .padding(horizontal = 6.dp)
                )
            }
        }

        // 顶部渐变淡出，让已唱过的歌词自然消隐
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
        )
        // 底部也加一层很淡的遮罩，避免最后几行紧贴滑杆
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                    )
                )
        )
    }
}
