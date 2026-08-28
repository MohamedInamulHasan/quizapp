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
    private var clockTickId: Int = 0
    private var fastTickSoundId: Int = 0

    fun init(context: Context) {
        if (soundPool != null) return
        try {
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
            fastTickSoundId = soundPool?.load(context, R.raw.clock_tick_fast, 1) ?: 0
            clockTickId = soundPool?.load(context, R.raw.clock_tick, 1) ?: 0
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

    // ⏱️ Urgent countdown ticking sound when timer reaches 5 seconds or below
    fun playFastUrgentTick() {
        if (!ThemeState.isSoundEnabled || soundPool == null) return
        try {
            val targetId = if (fastTickSoundId != 0) fastTickSoundId else clockTickId
            if (targetId != 0) {
                soundPool?.play(targetId, 1.0f, 1.0f, 1, 0, 1.2f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClockTick() {
        if (!ThemeState.isSoundEnabled || soundPool == null || clockTickId == 0) return
        try {
            soundPool?.play(clockTickId, 0.8f, 0.8f, 1, 0, 1.0f)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 💨 Question transition sound from raw media resource
    fun playWhooshSound() {
        if (!ThemeState.isSoundEnabled || soundPool == null) return
        try {
            val targetId = if (fastTickSoundId != 0) fastTickSoundId else clockTickId
            if (targetId != 0) {
                soundPool?.play(targetId, 0.8f, 0.8f, 1, 0, 1.5f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClickSound() {}
    fun playOptionPopSound() {}
    fun playSuccessChime() {}
}
