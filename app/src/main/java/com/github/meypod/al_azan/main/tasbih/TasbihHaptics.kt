package com.github.meypod.al_azan.main.tasbih

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants

/**
 * Provides crisp haptic feedback (light tick per count, distinct double-pulse on round completion)
 * and optional subtle system click sound for both in-app and floating Tasbih.
 */
object TasbihHaptics {
    fun performFeedback(
        context: Context,
        vibrationEnabled: Boolean,
        soundEnabled: Boolean,
        completedRound: Boolean,
    ) {
        if (vibrationEnabled) {
            vibrate(context, completedRound)
        }
        if (soundEnabled) {
            playClickSound(context)
        }
    }

    fun vibrateLight(context: Context) = vibrate(context, completedRound = false)

    private fun vibrate(
        context: Context,
        completedRound: Boolean,
    ) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (completedRound) {
                // Distinct celebratory double pulse on completing a round (e.g. 33 / 100)
                val timings = longArrayOf(0L, 55L, 65L, 95L)
                val amplitudes = intArrayOf(0, 180, 0, 240)
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                // Crisp subtle click tick on every tap
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (_: Exception) {
            // Safe fallback if vibrator is restricted
        }
    }

    private fun playClickSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(SoundEffectConstants.CLICK, 0.55f)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
