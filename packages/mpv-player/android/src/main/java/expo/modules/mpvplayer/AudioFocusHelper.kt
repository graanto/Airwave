package expo.modules.mpvplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Callbacks for AudioFocus events.
 */
interface AudioFocusListener {
  /** Focus lost permanently (e.g. another media app took over) or transiently (e.g. phone call). */
  fun onAudioFocusLost(permanent: Boolean)
  /** Focus regained after a transient loss. */
  fun onAudioFocusGained()
  /** Focus temporarily ducked (e.g. system notification or navigation prompt). */
  fun onAudioDuck(duck: Boolean)
}

/**
 * Manages Android AudioManager Audio Focus for libmpv playback.
 * Conforms to Android O+ (API 26+) AudioFocusRequest while supporting legacy fallbacks.
 */
class AudioFocusHelper(
  private val context: Context,
  private val listener: AudioFocusListener
) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var focusRequest: AudioFocusRequest? = null
  private var hasFocus = false
  private var pausedDueToTransientLoss = false

  private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        hasFocus = false
        pausedDueToTransientLoss = false
        listener.onAudioFocusLost(permanent = true)
        abandonFocus()
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        hasFocus = false
        pausedDueToTransientLoss = true
        listener.onAudioFocusLost(permanent = false)
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        listener.onAudioDuck(duck = true)
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        hasFocus = true
        listener.onAudioDuck(duck = false)
        if (pausedDueToTransientLoss) {
          pausedDueToTransientLoss = false
          listener.onAudioFocusGained()
        }
      }
    }
  }

  fun requestFocus(): Boolean {
    if (hasFocus) return true
    val am = audioManager ?: return false

    val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val playbackAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .build()
      val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(playbackAttributes)
        .setAcceptsDelayedFocusGain(false)
        .setOnAudioFocusChangeListener(focusChangeListener)
        .build()
      focusRequest = request
      am.requestAudioFocus(request)
    } else {
      @Suppress("DEPRECATION")
      am.requestAudioFocus(
        focusChangeListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN
      )
    }

    hasFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    return hasFocus
  }

  fun abandonFocus() {
    if (!hasFocus && focusRequest == null) return
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let { am.abandonAudioFocusRequest(it) }
      focusRequest = null
    } else {
      @Suppress("DEPRECATION")
      am.abandonAudioFocus(focusChangeListener)
    }
    hasFocus = false
    pausedDueToTransientLoss = false
  }
}
