package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import java.util.Collections

class SoundEffectsEngine(context: Context) {

  private val vibrator: Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

  @Volatile var soundEnabled: Boolean = true
  @Volatile var vibrationEnabled: Boolean = true

  private val loadedSoundIds: MutableSet<Int> = Collections.synchronizedSet(HashSet())

  private val soundPool: SoundPool? =
    try {
      SoundPool.Builder()
        .setMaxStreams(14)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .build()
        .apply {
          setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
              loadedSoundIds.add(sampleId)
            }
          }
        }
    } catch (_: Throwable) {
      null
    }

  private val buttonClickId = loadRaw(context, R.raw.sfx_button_click)
  private val pauseClickId = loadRaw(context, R.raw.sfx_pause_click)
  private val busTapId = loadRaw(context, R.raw.sfx_bus_tap)
  private val passengerPopId = loadRaw(context, R.raw.sfx_passenger_pop)
  private val busFullId = loadRaw(context, R.raw.sfx_bus_full)
  private val busCollisionId = loadRaw(context, R.raw.sfx_bus_collision)
  private val levelWinId = loadRaw(context, R.raw.sfx_level_win)
  private val unlockId = loadRaw(context, R.raw.sfx_unlock)
  private val levelLoseId = loadRaw(context, R.raw.sfx_level_lose)

  private fun loadRaw(context: Context, resId: Int): Int {
    val pool = soundPool ?: return 0
    return try {
      pool.load(context.applicationContext, resId, 1)
    } catch (_: Throwable) {
      0
    }
  }

  private fun playSample(soundId: Int, volume: Float = 1.0f, rate: Float = 1.0f) {
    if (!soundEnabled || soundId == 0) return
    val pool = soundPool ?: return
    try {
      pool.play(soundId, volume, volume, 1, 0, rate)
    } catch (_: Throwable) {
      // Ignore audio playback errors gracefully in constrained or headless environments
    }
  }

  fun playClick() {
    playSample(buttonClickId, volume = 0.95f)
  }

  fun playPauseClick() {
    playSample(pauseClickId, volume = 0.95f)
  }

  fun playBusTap() {
    playSample(busTapId, volume = 0.92f)
  }

  fun playTock() {
    playSample(busTapId, volume = 0.72f)
  }

  fun playBonk() {
    playSample(busCollisionId, volume = 0.95f)
    vibrate(45L)
  }

  fun playWhoosh() {
    playSample(busTapId, volume = 0.92f)
  }

  fun playPop(@Suppress("UNUSED_PARAMETER") combo: Int) {
    playSample(passengerPopId, volume = 0.90f, rate = 1.0f)
  }

  fun playDing() {
    playSample(busFullId, volume = 0.96f)
    vibrate(35L)
  }

  fun playUnlock() {
    playSample(unlockId, volume = 0.96f)
    vibrate(40L)
  }

  fun playWin() {
    playSample(levelWinId, volume = 1.0f)
    vibrate(70L)
  }

  fun playLose() {
    playSample(levelLoseId, volume = 0.92f)
    vibrate(90L)
  }

  fun vibrate(durationMs: Long) {
    if (!vibrationEnabled) return
    val v = vibrator ?: return
    if (!v.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        v.vibrate(durationMs)
      }
    } catch (_: SecurityException) {
      // Ignore if vibrator permission restricted on device
    }
  }

  fun release() {
    try {
      soundPool?.release()
    } catch (_: Throwable) {
      // Ignore release issues
    }
  }
}
