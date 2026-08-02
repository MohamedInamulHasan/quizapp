package com.ilygames.quizapp.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.ilygames.quizapp.R
import com.ilygames.quizapp.ui.theme.ThemeState
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object SoundManager {
    private var soundPool: SoundPool? = null
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var clockTickId: Int = 0
    private var slowWhooshSoundId: Int = 0

    fun init(context: Context) {
        if (soundPool != null) return
        try {
            ensureCustomAudioFilesExist(context)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()

            correctSoundId = soundPool?.load(context, R.raw.correct_sound, 1) ?: 0
            wrongSoundId = soundPool?.load(context, R.raw.wrong_sound, 1) ?: 0

            val tickFile = File(context.cacheDir, "crisp_clock_tick.wav")
            if (tickFile.exists()) {
                clockTickId = soundPool?.load(tickFile.absolutePath, 1) ?: 0
            }

            val whooshFile = File(context.cacheDir, "slow_whoosh.wav")
            if (whooshFile.exists()) {
                slowWhooshSoundId = soundPool?.load(whooshFile.absolutePath, 1) ?: 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureCustomAudioFilesExist(context: Context) {
        try {
            // 1. Slow Whoosh Sound Generator (380ms Smooth Aerodynamic Air Sweep)
            val whooshFile = File(context.cacheDir, "slow_whoosh.wav")
            if (!whooshFile.exists()) {
                generateWavFile(
                    file = whooshFile,
                    duration = 0.38,
                    sampleRate = 44100
                ) { i, numSamples, sampleRate ->
                    val t = i.toDouble() / sampleRate
                    // Smooth S-curve envelope over 380ms
                    val env = Math.sin((t / 0.38) * Math.PI)
                    val random = java.util.Random(i.toLong())
                    val noise = random.nextDouble() * 2.0 - 1.0
                    val freq = 90.0 + 130.0 * Math.sin(t * Math.PI / 0.38)
                    val tone = Math.sin(2 * Math.PI * freq * t)
                    ((noise * 0.55 + tone * 0.45) * env * 18000).toInt()
                }
            }

            // 2. Crisp Mechanical Clock Tick Generator (35ms Sharp Pendulum Gear Click)
            val tickFile = File(context.cacheDir, "crisp_clock_tick.wav")
            if (!tickFile.exists()) {
                generateWavFile(
                    file = tickFile,
                    duration = 0.035,
                    sampleRate = 44100
                ) { i, numSamples, sampleRate ->
                    val t = i.toDouble() / sampleRate
                    val decay = Math.exp(-t * 120.0) // Fast exponential damping
                    val tone = Math.sin(2 * Math.PI * 850.0 * t)
                    val noise = (java.util.Random(i.toLong()).nextDouble() * 2.0 - 1.0) * 0.3
                    ((tone + noise) * decay * 24000).toInt()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inline fun generateWavFile(
        file: File,
        duration: Double,
        sampleRate: Int,
        sampleGenerator: (i: Int, numSamples: Int, sampleRate: Int) -> Int
    ) {
        val numSamples = (sampleRate * duration).toInt()
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // Mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (i in 0 until numSamples) {
            val sample = sampleGenerator(i, numSamples, sampleRate)
            val clamped = Math.max(-32768, Math.min(32767, sample)).toShort()
            buffer.putShort(clamped)
        }

        FileOutputStream(file).use { fos ->
            fos.write(buffer.array())
        }
    }

    fun playCorrectSound() {
        if (!ThemeState.isSoundEnabled || soundPool == null || correctSoundId == 0) return
        try {
            soundPool?.play(correctSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playWrongSound() {
        if (!ThemeState.isSoundEnabled || soundPool == null || wrongSoundId == 0) return
        try {
            soundPool?.play(wrongSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ⏱️ Silent per user directive
    fun playFastUrgentTick() {}

    // 💨 Silent per user directive
    fun playWhooshSound() {}

    fun playClockTick() {}
    fun playClickSound() {} // Removed button click sound per user request
    fun playOptionPopSound() {} // Removed card click sound per user request
    fun playSuccessChime() {}
}
