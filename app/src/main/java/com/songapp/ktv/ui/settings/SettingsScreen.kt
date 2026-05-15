package com.songapp.ktv.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.songapp.ktv.KtvApp
import com.songapp.ktv.ui.components.GlassSurface
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonGold
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import java.io.File

@Composable
fun SettingsScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val app = KtvApp.get()
    val count by app.repository.count().collectAsState(initial = 0)

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        Header()
        Spacer(Modifier.height(8.dp))
        InfoCard(
            icon = Icons.Filled.Folder,
            tint = NeonCyan,
            title = "本地存储",
            subtitle = "曲库目录：${songsDir(ctx)}\n已收录：$count 首歌"
        )
        Spacer(Modifier.height(10.dp))
        InfoCard(
            icon = Icons.Filled.AutoAwesome,
            tint = NeonGold,
            title = "原伴唱切换",
            subtitle = "当前版本使用立体声中央声道消除算法实时处理。下一版将引入端侧 AI 流式人声分离，分离后双轨持久缓存到本地，秒切原伴唱。"
        )
        Spacer(Modifier.height(10.dp))
        InfoCard(
            icon = Icons.Filled.AudioFile,
            tint = NeonViolet,
            title = "支持的格式",
            subtitle = "MP3 / M4A / AAC / FLAC / OGG（凡 ExoPlayer 支持的均可）。歌词支持同名 .lrc 文件，导入时会自动收集。"
        )
        Spacer(Modifier.height(10.dp))
        InfoCard(
            icon = Icons.Filled.Info,
            tint = NeonPink,
            title = "关于 星唱 KTV",
            subtitle = "v0.1.0  ·  本地优先 K 歌应用，所有数据保存在本设备。"
        )
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
                .background(Brush.linearGradient(listOf(NeonGold, NeonPink))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.Black) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("设置", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("应用信息与说明", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tint) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun songsDir(ctx: android.content.Context): String =
    File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "songs").absolutePath
