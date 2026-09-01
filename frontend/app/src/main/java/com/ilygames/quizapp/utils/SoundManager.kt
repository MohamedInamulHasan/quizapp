package com.ilygames.quizapp.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.ilygames.quizapp.R
import com.ilygames.quizapp.ui.theme.ThemeState

object SoundManager {
    private var soundPool: SoundPool? = null
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var fastTickSoundId: Int = 0
    private var clickSoundId: Int = 0
    private var swooshSoundId: Int = 0

    fun init(context: Context) {
        if (soundPool != null) return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(audioAttributes)
                .build()

            correctSoundId = soundPool?.load(context, R.raw.correct_sound, 1) ?: 0
            wrongSoundId = soundPool?.load(context, R.raw.wrong_sound, 1) ?: 0
            fastTickSoundId = soundPool?.load(context, R.raw.clock_tick_fast, 1) ?: 0
            clickSoundId = soundPool?.load(context, R.raw.tap, 1) ?: 0
            swooshSoundId = soundPool?.load(context, R.raw.swoosh, 1) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
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

    // ⏱️ Fast urgent ticking sound effect
    fun playFastUrgentTick() {
        if (!ThemeState.isSoundEnabled || soundPool == null || fastTickSoundId == 0) return
        try {
            soundPool?.play(fastTickSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🎵 Custom Tap sound effect for card clicks, buttons, and options
    fun playClickSound() {
        if (!ThemeState.isSoundEnabled || soundPool == null || clickSoundId == 0) return
        try {
            soundPool?.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 💨 Custom Swoosh sound effect when advancing from 2nd question onwards
    fun playWhooshSound() {
        if (!ThemeState.isSoundEnabled || soundPool == null || swooshSoundId == 0) return
        try {
            soundPool?.play(swooshSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClockTick() {}
    fun playOptionPopSound() {}
    fun playSuccessChime() {}
}
