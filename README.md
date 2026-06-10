# 星唱 KTV (SongKtv)

本地优先的 Android 家庭 KTV 工具：手机负责播放与实时伴唱处理，电视显示大歌词，其他手机扫码点歌。

## 功能

- 🎤 **原伴唱实时切换**：基于中央声道消除和中置基频压制的实时算法，滑杆 0~1 线性混合，零延迟
- 🎼 **本地 MP3 导入**：保留 ID3 元数据、自动捕获专辑封面
- 📜 **同源歌词**：下载时从同一歌源记录保存音频与 `.lrc`，也支持手动导入 `.lrc` / `.txt`
- 🌐 **歌源订阅**：支持用户自备 JSON 歌库链接，App 负责搜索、下载和本地管理
- 📺 **家庭 KTV 房间**：局域网电视大屏歌词页 + 扫码点歌页
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
