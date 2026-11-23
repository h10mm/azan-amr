package com.github.meypod.al_azan.core.data.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reports a hardware volume-key press while a sound plays, so the caller can treat it as a dismiss.
 * Shared by both playback paths: the foreground-service alarm and the short "soft" sounds.
 *
 * The reliable mechanism is an active [MediaSession] with a remote [VolumeProvider]: the framework
 * routes the volume keys to it at the audio layer — before they reach any window or change a stream —
 * so a press registers even with the screen off or locked, the case the VOLUME_CHANGED broadcast never
 * reaches. Because the provider owns the volume, an edge press still fires an onAdjust callback, so no
 * volume nudging is needed. That broadcast is only the fallback for when the session can't be created.
 *
 * The audio itself is played by the caller; this session exists to capture the keys and, when
 * [nowPlaying] is given, to surface the sound as now-playing metadata on the lock screen / Auto / BT.
 *
 * [onDismiss] may be invoked from the main thread or a broadcast thread, and at most once — the
 * monitor disarms itself before reporting.
 */
class VolumeKeyDismissMonitor(
    private val context: Context,
    private val nowPlaying: NowPlaying? = null,
    private val onDismiss: () -> Unit,
) {
    /** Lock-screen / Auto / Bluetooth "now playing" labels. Omitted for sounds too short to be worth surfacing. */
    data class NowPlaying(
        val title: String,
        val subtitle: String,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val dismissed = AtomicBoolean(false)
    private var startedAtRealtime = 0L
    private var session: MediaSession? = null
    private var receiver: BroadcastReceiver? = null

    fun start() {
        startedAtRealtime = SystemClock.elapsedRealtime()
        session = createSession()
        // Register the volume broadcast receiver as well so that when playing on STREAM_ALARM (where
        // OEM skins like MIUI route hardware volume keys to STREAM_ALARM instead of MediaSession),
        // pressing Volume Up or Volume Down still immediately dismisses the Adhan.
        registerVolumeReceiver()
    }

    fun stop() {
        session?.let {
            runCatching { it.isActive = false }
            runCatching { it.release() }
        }
        session = null
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }

    private fun dismiss() {
        if (!dismissed.compareAndSet(false, true)) return
        stop()
        onDismiss()
    }

    private fun createSession(): MediaSession? {
        val session = runCatching { MediaSession(context, SESSION_TAG) }.getOrNull() ?: return null
        val provider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, VOLUME_PROVIDER_MAX, VOLUME_PROVIDER_MAX / 2) {
            override fun onAdjustVolume(direction: Int) {
                if (SystemClock.elapsedRealtime() - startedAtRealtime < 1500L) return
                if (direction != 0) dismiss() // any up/down press dismisses
            }

            override fun onSetVolumeTo(volume: Int) {
                // Do not dismiss on absolute volume set: system audio ducking / notification events call this.
            }
        }
        val armed = runCatching {
            // A callback (with an explicit main-thread handler) must exist for the platform MediaSession
            // to have a message handler to dispatch incoming volume adjustments onto — without it the
            // framework routes the key to our session but the VolumeProvider callback is silently dropped.
            session.setCallback(
                object : MediaSession.Callback() {
                    // Do NOT override onStop/onPause to dismiss: incoming notifications or other media apps
                    // (e.g. Reels/WhatsApp) cause the system to send onStop to active MediaSessions.
                },
                mainHandler,
            )
            nowPlaying?.let {
                session.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, it.title)
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, it.title)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, it.subtitle)
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, it.subtitle)
                        .build(),
                )
            }
            session.setPlaybackToRemote(provider)
            // A PLAYING state makes this the active session the framework routes volume keys to.
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build(),
            )
            session.isActive = true
        }.isSuccess
        if (!armed) {
            runCatching { session.release() }
            return null
        }
        return session
    }

    private fun registerVolumeReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                c: Context?,
                i: Intent?,
            ) {
                if (i == null) return
                // Ignore initial stream setup broadcasts during the first 1500ms of playback.
                if (SystemClock.elapsedRealtime() - startedAtRealtime < 1500L) return
                val streamType = i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType != AudioManager.STREAM_ALARM) return
                val newVol = i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                val prevVol = i.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                if (newVol == -1 || prevVol == -1 || newVol == prevVol) return
                dismiss()
            }
        }
        this.receiver = receiver
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_VOLUME_CHANGED),
            // EXPORTED: some OEMs won't deliver the system VOLUME_CHANGED broadcast to a NOT_EXPORTED
            // runtime receiver while the screen is off. VOLUME_CHANGED_ACTION is a protected broadcast
            // (only the system can send it), so exporting doesn't let other apps spoof a dismiss.
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    companion object {
        /** Not public API, but the only signal that a stream level changed. */
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"

        private const val SESSION_TAG = "alarm-volume"

        /** Arbitrary range; only the fact that a press arrives matters, never the value. */
        private const val VOLUME_PROVIDER_MAX = 100
    }
}
