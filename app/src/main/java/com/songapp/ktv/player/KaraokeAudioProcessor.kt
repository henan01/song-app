package com.songapp.ktv.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * 改良版"原唱/伴唱"实时混音器。
 *
 * 思路：
 *   - 取中央声道 mid = (L + R) / 2
 *   - 对 mid 做一阶低通滤波得到 mid_low（低频，保留贝斯/底鼓等位于中央的低音乐器）
 *   - mid_high = mid - mid_low（高频中央内容，主要是人声）
 *   - 输出时主要减去 mid_high，再轻压一部分完整 mid，覆盖人声基频残留
 *
 * 因此滑杆 0 时：人声大幅消除，低音/伴奏会有少量损耗但比旧版残留更少
 *      滑杆 1 时：完全保持原信号
 *      中间值线性插值，体感是"人声大小"而不是"整体音量"。
 */
class KaraokeAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var vocalLevel: Float = 1.0f

    private var lpfState: Float = 0f
    private var lpfAlpha: Float = 0.978f
    private val cutoffHz: Float = 170f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        val sr = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        lpfAlpha = exp(-2.0 * Math.PI * cutoffHz / sr).toFloat()
        lpfState = 0f
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channelCount = inputAudioFormat.channelCount
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        out.order(ByteOrder.nativeOrder())
        val src = inputBuffer.order(ByteOrder.nativeOrder())
        val v = vocalLevel.coerceIn(0f, 1f)
        val cancelStrength = 1f - v   // 1 时全消，0 时不动
        val a = lpfAlpha
        val invA = 1f - a
        when (channelCount) {
            2 -> {
                val frames = size / 4
                var lp = lpfState
                for (i in 0 until frames) {
                    val l = src.short.toInt()
                    val r = src.short.toInt()
                    val mid = (l + r) * 0.5f
                    lp = a * lp + invA * mid
                    val midHigh = mid - lp
                    val foundation = mid * 0.22f
                    val toRemove = cancelStrength * (midHigh + foundation)
                    val outL = (l - toRemove).toInt().coerceIn(-32768, 32767)
                    val outR = (r - toRemove).toInt().coerceIn(-32768, 32767)
                    out.putShort(outL.toShort())
                    out.putShort(outR.toShort())
                }
                lpfState = lp
            }
            1 -> out.put(src)
            else -> out.put(src)
        }
        out.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    override fun onReset() {
        lpfState = 0f
    }
}
