# 星唱 KTV 功能验证报告

验证日期：2026-06-10

## 本次实现范围

- 去掉网易云入口，默认仅保留酷我过渡源。
- 新增用户自备 JSON 歌源订阅，支持搜索、下载、封面、歌词。
- 新增本地 `catalog.json` 文件导入，不再必须手动搭 HTTP 服务。
- JSON 歌源支持 `baseUrl` + 相对路径，降低维护成本。
- 新增“开唱”首页，底部导航调整为：开唱 / 曲库 / 歌源 / 点歌 / 我的。
- 下载时音频和歌词来自同一条歌源记录，播放器不再自动跨平台搜索歌词。
- 新增家庭 KTV 局域网房间：
  - `/tv`：电视大屏歌词页。
  - `/remote`：手机扫码点歌页。
  - `/api/state`、`/api/songs`、`/api/enqueue`、`/api/play`、`/api/toggle`、`/api/next`。
- 设置页新增“开启投屏/扫码点歌”、电视地址、点歌二维码。
- 实时伴唱模式增强：在原中央高频压制基础上，增加轻量中置基频压制，降低人声残留。

## 歌源订阅格式

支持对象或数组两种格式：

```json
{
  "name": "家庭歌库",
  "baseUrl": "https://example.com/music/",
  "tracks": [
    {
      "id": "jay-chou-qingtian",
      "title": "晴天",
      "artist": "周杰伦",
      "album": "叶惠美",
      "durationMs": 269000,
      "mediaUrl": "qingtian.mp3",
      "lyricUrl": "qingtian.lrc",
      "coverUrl": "qingtian.jpg"
    }
  ]
}
```

字段兼容：

- 音频：`mediaUrl` / `mp3Url` / `audioUrl`
- 歌词：`lyricUrl` / `lrcUrl`
- 歌名：`title` / `name`
- 歌手：`artist` / `singer`
- 封面：`coverUrl` / `cover`

## 验证方法

1. 构建验证

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

期望结果：`BUILD SUCCESSFUL`。

2. 歌源订阅验证

- 打开 App 的“歌源”页。
- 方式 A：点击“选择本地 catalog.json”，选择 JSON 文件。
- 方式 B：输入一个合法的 `catalog.json` 链接，然后点击“添加”。
- 搜索订阅里的歌曲标题或歌手。
- 点击下载。

期望结果：

- 能看到“自备”来源标识。
- 本地 JSON 和 URL 订阅都能成为可搜索歌源。
- 下载后本地目录生成 `original.mp3`。
- 如果订阅记录包含 `lyricUrl`，同目录生成 `lyrics.lrc`。
- 播放该歌曲时歌词使用下载时保存的同源 `lyrics.lrc`。

3. 网易云移除验证

- 打开“歌源”页。
- 查看音乐源筛选区域。
- 播放一首没有歌词的歌曲。

期望结果：

- 不再出现“网易云”筛选项。
- 播放器顶部不再出现“在线搜索歌词”按钮。
- 无歌词时提示“暂无同源歌词，可手动导入 .lrc”。

4. 家庭 KTV 房间验证

- 打开“设置”页。
- 点击“开启投屏/扫码点歌”。
- 用同一局域网内的电视/电脑浏览器打开电视地址，例如 `http://手机IP:8989/tv`。
- 用另一台手机扫码或打开点歌地址，例如 `http://手机IP:8989/remote`。

期望结果：

- 电视页显示当前歌曲、大歌词、下一句歌词和播放进度。
- 点歌页可以搜索本机曲库。
- 点击“点歌”后歌曲进入队列。
- 点击“唱”后主机手机开始播放该歌曲，电视页同步刷新。
- 点歌页的“播放/暂停”“下一首”可控制主机播放。

5. 伴唱增强验证

- 播放一首立体声人声居中的歌曲。
- 在播放器里把“原唱/伴唱”滑杆拉到伴唱侧。
- 对比旧版或滑杆原唱侧。

期望结果：

- 中置人声比旧版更弱。
- 伴奏会有轻微音色损耗，这是实时算法限制。
- 该能力仍不是 AI 人声分离，后续如需接近 KTV 伴奏效果，需要接端侧模型做离线预处理。

## 已执行验证

- 已执行 Debug 构建：

```text
BUILD SUCCESSFUL in 3s
36 actionable tasks: 6 executed, 30 up-to-date
```

## 未覆盖风险

- 未在真实 Android 设备、电视浏览器、不同路由器隔离策略下做端到端实测。
- 酷我仍是过渡源，存在接口变动和版权边界风险；更推荐上线时默认只启用用户自备订阅源。
- 实时伴唱无法完全消除人声，复杂混音、单声道、人声非中置的歌曲效果会明显变差。
