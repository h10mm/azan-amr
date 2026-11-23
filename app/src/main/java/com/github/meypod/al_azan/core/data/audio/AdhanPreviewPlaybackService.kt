package com.github.meypod.al_azan.core.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.audio.AdhanPreviewPlaybackService.Companion.playingId
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that previews a single adhan/audio entry. Holds a [MediaPlayer], shows an
 * ongoing notification with a Stop action while playing, and yields to phone calls via audio focus
 * and telephony state. Only one entry plays at a time; [playingId] mirrors the active entry id.
 */
class AdhanPreviewPlaybackService :
    Service(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val ACTION_PLAY = "com.github.meypod.al_azan.action.PREVIEW_PLAY"
        private const val ACTION_PLAY_ADHAN = "com.github.meypod.al_azan.action.ADHAN_ALARM_PLAY"
        private const val ACTION_STOP = "com.github.meypod.al_azan.action.PREVIEW_STOP"
        private const val ACTION_SET_VOLUME = "com.github.meypod.al_azan.action.PREVIEW_SET_VOLUME"
        private const val EXTRA_URI = "uri"
        private const val EXTRA_ID = "id"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_LOOP = "loop"
        private const val EXTRA_VOLUME_PERCENT = "volume_percent"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"

        private const val CHANNEL_ID = "adhan_preview_playback"
        private const val NOTIFICATION_ID = 0xADA1

        @Volatile
        private var fallbackPlayer: MediaPlayer? = null

        @Volatile
        private var fallbackWakeLock: PowerManager.WakeLock? = null

        private val _playingId = MutableStateFlow<String?>(null)

        /** Emits the id of the entry currently previewing, or null when idle. Process-global. */
        val playingId: StateFlow<String?> = _playingId.asStateFlow()

        fun playAdhanAlarm(
            context: Context,
            uri: Uri,
            volumePercent: Int = -1,
        ) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, AdhanPreviewPlaybackService::class.java).apply {
                action = ACTION_PLAY_ADHAN
                putExtra(EXTRA_URI, uri.toString())
                putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
            }
            val startedService = runCatching {
                appCtx.startService(intent)
            }.isSuccess
            if (!startedService) {
                playAdhanAlarmInProcess(appCtx, uri, volumePercent)
            }
        }

        private fun playAdhanAlarmInProcess(
            appCtx: Context,
            uri: Uri,
            volumePercent: Int,
        ) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                runCatching {
                    fallbackPlayer?.let {
                        runCatching { it.reset() }
                        runCatching { it.release() }
                    }
                    fallbackPlayer = null
                    fallbackWakeLock?.let { if (it.isHeld) runCatching { it.release() } }
                    val pm = appCtx.getSystemService<PowerManager>()
                    fallbackWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "al_azan:adhan_fallback_wakelock")?.apply {
                        setReferenceCounted(false)
                        runCatching { acquire(5 * 60 * 1000L) }
                    }
                    val am = appCtx.getSystemService<AudioManager>()
                    val savedVol = if (volumePercent in 0..100 && am != null) {
                        val cur = runCatching { am.getStreamVolume(AudioManager.STREAM_ALARM) }.getOrDefault(-1)
                        val max = runCatching { am.getStreamMaxVolume(AudioManager.STREAM_ALARM) }.getOrDefault(0)
                        if (max > 0) {
                            runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, (volumePercent * max + 50) / 100, 0) }
                        }
                        cur
                    } else {
                        -1
                    }
                    val cleanupFallback = {
                        if (savedVol >= 0 && am != null) {
                            runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, savedVol, 0) }
                        }
                        fallbackPlayer?.let {
                            runCatching { it.reset() }
                            runCatching { it.release() }
                        }
                        fallbackPlayer = null
                        fallbackWakeLock?.let { if (it.isHeld) runCatching { it.release() } }
                        fallbackWakeLock = null
                    }
                    fallbackPlayer = MediaPlayer().apply {
                        setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                        )
                        setOnPreparedListener { mp -> runCatching { mp.start() } }
                        setOnCompletionListener { cleanupFallback() }
                        setOnErrorListener { _, _, _ ->
                            cleanupFallback()
                            true
                        }
                        isLooping = false
                        setDataSource(appCtx, uri)
                        prepareAsync()
                    }
                }
            }
        }

        fun play(
            context: Context,
            uri: Uri,
            id: String,
            label: String,
            loop: Boolean = false,
            volumePercent: Int = -1,
        ) {
            val intent = Intent(context, AdhanPreviewPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_URI, uri.toString())
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_LOOP, loop)
                putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Live-adjusts the playing preview's volume. Callers should only send this while playing. */
        fun setVolume(
            context: Context,
            volumePercent: Int,
        ) {
            val intent = Intent(context, AdhanPreviewPlaybackService::class.java).apply {
                action = ACTION_SET_VOLUME
                putExtra(EXTRA_VOLUME_PERCENT, volumePercent)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPreviewPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            // already-stopped service: a plain startService is fine, onStartCommand cleans up.
            context.startService(intent)
        }
    }

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var volumeKeyMonitor: VolumeKeyDismissMonitor? = null
    private var adhanWakeLock: PowerManager.WakeLock? = null
    private var isAdhanAlarm: Boolean = false
    private var activeNotificationId: Int = NOTIFICATION_ID

    // 0..100 sets the media stream volume absolutely (matching how the alarm playback applies the
    // custom volume); -1 leaves the device volume untouched. Applied on prepare and on
    // ACTION_SET_VOLUME so slider drags adjust a running preview live.
    private var volumePercent = -1

    // Media-stream level before the preview overrode it, restored on stop; -1 = nothing to restore.
    private var savedStreamVolume = -1

    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    private val audioManager: AudioManager? by lazy { getSystemService() }
    private val telephonyManager: TelephonyManager? by lazy { getSystemService() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_PLAY_ADHAN -> {
                val uri = intent.getStringExtra(EXTRA_URI)?.toUri()
                volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, -1)
                if (uri != null) {
                    startAdhanAlarmPlayback(uri)
                } else {
                    cleanupAndStop(cancelNotification = false)
                }
            }

            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI)?.toUri()
                val id = intent.getStringExtra(EXTRA_ID)
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                val loop = intent.getBooleanExtra(EXTRA_LOOP, false)
                volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, -1)
                if (uri != null && id != null) {
                    startPlayback(uri, id, label, loop)
                } else {
                    cleanupAndStop(cancelNotification = true)
                }
            }

            ACTION_SET_VOLUME -> {
                volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, -1)
                // A set-volume racing a finished preview starts the service with no player; stop again
                // instead of lingering as a started-but-idle service.
                if (player != null) applyStreamVolume() else cleanupAndStop(cancelNotification = true)
            }

            else -> cleanupAndStop(cancelNotification = true)
        }
        return START_NOT_STICKY
    }

    /**
     * Custom volume is absolute: sets the media stream to [volumePercent] of its max, independent of
     * the device volume. The pre-existing level is saved once and restored on stop.
     */
    private fun applyStreamVolume() {
        if (volumePercent !in 0..100) return
        val am = audioManager ?: return
        val stream = if (isAdhanAlarm) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
        if (savedStreamVolume == -1) {
            savedStreamVolume = runCatching { am.getStreamVolume(stream) }.getOrDefault(-1)
        }
        val max = runCatching { am.getStreamMaxVolume(stream) }.getOrDefault(0)
        if (max <= 0) return
        runCatching { am.setStreamVolume(stream, (volumePercent * max + 50) / 100, 0) }
    }

    private fun restoreStreamVolume() {
        if (savedStreamVolume < 0) return
        val stream = if (isAdhanAlarm) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
        runCatching { audioManager?.setStreamVolume(stream, savedStreamVolume, 0) }
        savedStreamVolume = -1
    }

    private fun startAdhanAlarmPlayback(
        uri: Uri,
    ) {
        isAdhanAlarm = true

        if (isCallActive()) {
            cleanupAndStop(cancelNotification = false)
            return
        }
        registerCallStateListener()

        adhanWakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        val pm = getSystemService<PowerManager>()
        adhanWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "al_azan:adhan_playback")?.apply {
            setReferenceCounted(false)
            runCatching { acquire(5 * 60 * 1000L) }
        }

        volumeKeyMonitor?.stop()
        volumeKeyMonitor = null

        player?.release()
        player = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnPreparedListener(this@AdhanPreviewPlaybackService)
            setOnCompletionListener(this@AdhanPreviewPlaybackService)
            setOnErrorListener(this@AdhanPreviewPlaybackService)
            isLooping = false
            val ok = runCatching {
                setDataSource(applicationContext, uri)
                prepareAsync()
            }.isSuccess
            if (!ok) {
                cleanupAndStop(cancelNotification = false)
                return
            }
        }
    }

    private fun startPlayback(
        uri: Uri,
        id: String,
        label: String,
        loop: Boolean,
    ) {
        isAdhanAlarm = false
        activeNotificationId = NOTIFICATION_ID
        // Must call startForeground promptly after startForegroundService.
        startForeground(NOTIFICATION_ID, buildNotification(label))

        if (isCallActive()) {
            cleanupAndStop(cancelNotification = true)
            return
        }
        if (!requestAudioFocus()) {
            cleanupAndStop(cancelNotification = true)
            return
        }
        registerCallStateListener()

        player?.release()
        player = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnPreparedListener(this@AdhanPreviewPlaybackService)
            setOnCompletionListener(this@AdhanPreviewPlaybackService)
            setOnErrorListener(this@AdhanPreviewPlaybackService)
            isLooping = loop
            val ok = runCatching {
                setDataSource(applicationContext, uri)
                prepareAsync()
            }.isSuccess
            if (!ok) {
                cleanupAndStop(cancelNotification = true)
                return
            }
        }
        _playingId.value = id
    }

    override fun onPrepared(mp: MediaPlayer) {
        applyStreamVolume()
        runCatching { mp.start() }
    }

    override fun onCompletion(mp: MediaPlayer) = cleanupAndStop(cancelNotification = !isAdhanAlarm)

    override fun onError(
        mp: MediaPlayer,
        what: Int,
        extra: Int,
    ): Boolean {
        cleanupAndStop(cancelNotification = !isAdhanAlarm)
        return true
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // Only stop in-app previews on audio focus loss; real Adhan alarms play on USAGE_ALARM and
        // must not be cut off by transient notifications or touch sounds.
        if (!isAdhanAlarm && focusChange <= 0) cleanupAndStop(cancelNotification = true)
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this)
            .build()
        focusRequest = request
        return am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun isCallActive(): Boolean {
        if (!hasPhoneStatePermission()) return false
        val tm = telephonyManager ?: return false

        @Suppress("DEPRECATION")
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tm.callStateForSubscription else tm.callState
        return state != TelephonyManager.CALL_STATE_IDLE
    }

    private fun registerCallStateListener() {
        if (!hasPhoneStatePermission()) return
        val tm = telephonyManager ?: return
        // A re-issued ACTION_PLAY hits startPlayback again; drop any prior registration first so
        // callbacks don't accumulate and trip the platform's per-pid listener cap.
        unregisterCallStateListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = onCallState(state)
            }
            telephonyCallback = callback
            runCatching { tm.registerTelephonyCallback(mainExecutor, callback) }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(
                    state: Int,
                    phoneNumber: String?,
                ) = onCallState(state)
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            runCatching { tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE) }
        }
    }

    private fun unregisterCallStateListener() {
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { tm.unregisterTelephonyCallback(it) }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
            phoneStateListener = null
        }
    }

    private fun onCallState(state: Int) {
        if (state != TelephonyManager.CALL_STATE_IDLE) cleanupAndStop(cancelNotification = false)
    }

    private fun buildNotification(label: String): android.app.Notification {
        // Service contexts don't carry the per-app locale on pre-API 33; resolve strings localized.
        // The preview only ever starts from in-app UI, so the AppCompat locale store is written and
        // the parameterless lookup is reliable here.
        val localized = withAppLocale()
        ensureChannel(localized)
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, AdhanPreviewPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_notif)
            .setContentTitle(localized.getString(R.string.adhan_preview_playing))
            .setContentText(label)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(R.drawable.outline_stop_24, localized.getString(R.string.stop), stopIntent)
            .build()
    }

    private fun ensureChannel(localized: Context) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW,
        )
            .setName(localized.getString(R.string.adhan_preview_channel_name))
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun cleanupAndStop(cancelNotification: Boolean = true) {
        volumeKeyMonitor?.stop()
        volumeKeyMonitor = null
        player?.let { mp ->
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        player = null
        adhanWakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        adhanWakeLock = null
        // Before abandoning focus so whatever resumes after us hears the restored level.
        restoreStreamVolume()
        abandonAudioFocus()
        unregisterCallStateListener()
        _playingId.value = null
        if (!isAdhanAlarm) {
            if (cancelNotification) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                NotificationManagerCompat.from(this).cancel(activeNotificationId)
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
        stopSelf()
    }

    override fun onDestroy() {
        cleanupAndStop(cancelNotification = false)
        super.onDestroy()
    }
}
