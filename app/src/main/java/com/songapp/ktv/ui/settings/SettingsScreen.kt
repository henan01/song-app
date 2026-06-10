package com.songapp.ktv.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.songapp.ktv.KtvApp
import com.songapp.ktv.ui.components.GlassSurface
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonGold
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

@Composable
fun SettingsScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val app = KtvApp.get()
    val count by app.repository.count().collectAsState(initial = 0)
    var roomRunning by remember { mutableStateOf(app.roomServer.isRunning) }

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
        RoomCard(
            running = roomRunning,
            tvUrl = app.roomServer.tvUrl,
            remoteUrl = app.roomServer.remoteUrl,
            onStart = {
                app.roomServer.start()
                roomRunning = app.roomServer.isRunning
            },
            onStop = {
                app.roomServer.stop()
                roomRunning = app.roomServer.isRunning
            }
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
            subtitle = "v0.1.0  ·  家庭 KTV 工具，手机负责播放与处理，电视/其他手机通过局域网协作。"
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

@Composable
private fun RoomCard(
    running: Boolean,
    tvUrl: String,
    remoteUrl: String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(NeonCyan.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.LiveTv, contentDescription = null, tint = NeonCyan) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("家庭 KTV 房间", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (running) "电视打开：$tvUrl\n扫码或访问：$remoteUrl"
                        else "开启后，电视显示大歌词，其他手机扫码进入局域网点歌。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    val bitmap = remember(remoteUrl) { qrBitmap(remoteUrl, 196) }
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "扫码点歌", modifier = Modifier.size(112.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("扫码点歌", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onStop) { Text("关闭房间") }
                    }
                } else {
                    Button(onClick = onStart) { Text("开启投屏/扫码点歌") }
                }
            }
        }
    }
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

private fun songsDir(ctx: android.content.Context): String =
    File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "songs").absolutePath
