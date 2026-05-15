# 星唱 KTV (SongKtv)

本地优先的 Android KTV 播放器：导入普通 MP3，一根滑杆切换原唱/伴唱，歌词自动联网下载、本地缓存。

## 功能

- 🎤 **原伴唱实时切换**：基于中央声道高通消音的实时算法，滑杆 0~1 线性混合，零延迟
- 🎼 **本地 MP3 导入**：保留 ID3 元数据、自动捕获专辑封面
- 📜 **歌词**：自动联网搜索（网易云开放接口）+ 手动导入 `.lrc` / `.txt`，本地缓存
- 🔍 **搜索 / 分页 / 筛选 / 排序**：歌名/歌手子串匹配，按导入时间/歌名/最常播放排序
- 📋 **点歌队列**：拖动排序、自动连播
- ⭐ **收藏 / 正在播放快捷栏**：曲库顶部直接看到当前播放
- 🌌 **霓虹极光主题**：Material 3 + 动态背景、玻璃拟态卡片

## 下载

到 [Releases](../../releases) 直接下载最新 APK 安装。

## 自己构建

需要 JDK 17 + Android SDK (API 34) + 命令行 Gradle 8.5。

```bash
gradle assembleDebug
# APK 在 app/build/outputs/apk/debug/app-debug.apk
```

或者直接 Android Studio 打开本目录 → Sync → Run。

## 发布新版本

打 tag 到 GitHub，CI 自动构建并把 APK 发到 Releases：

```bash
git tag -a v0.2.0 -m "v0.2.0"
git push origin v0.2.0
```

## 技术栈

Kotlin · Jetpack Compose (Material 3) · Room · Paging 3 · Media3 ExoPlayer · Coroutines · WorkManager

## 现状

当前版本属于 MVP，"原伴唱切换"用的是立体声中央声道高通消音算法——人声居中混音的歌效果好，现代电子乐人声残留较多。下一阶段计划接入端侧 Spleeter / Demucs TFLite 做真 AI 分离，详见 [PRD.md](PRD.md)。

## License

MIT
