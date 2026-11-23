package com.github.meypod.al_azan.main.tasbih

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Foreground Service displaying a Material 3 styled, theme-aware draggable Floating Tasbih Bubble
 * over other apps using [WindowManager], complete with progress ring, tap-to-count, and drag-down-to-close target.
 */
@AndroidEntryPoint
class FloatingTasbihService : Service() {

    @Inject
    lateinit var tasbihRepository: TasbihRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var bubbleView: FloatingTasbihBubbleView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var dismissZoneView: DismissDropZoneView? = null
    private var dismissZoneParams: WindowManager.LayoutParams? = null
    private var dismissZoneShown = false
    private var hoveredDismiss = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        tasbihRepository.setFloatingRunning(true)

        if (bubbleView == null) {
            attachBubbleOverlay()
            observeState()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        tasbihRepository.setFloatingRunning(false)
        serviceScope.cancel()
        removeOverlays()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "المسبحة العائمة",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "إشعار لإبقاء فقاعة المسبحة العائمة تعمل بسلاسة"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingTasbihService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tasbih)
            .setContentTitle("المسبحة العائمة نشطة")
            .setContentText("اضغط على الفقاعة للتسبيح، أو اسحبها للأسفل لإغلاقها")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.baseline_close_24, "إغلاق", stopIntent)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun observeState() {
        serviceScope.launch {
            combine(tasbihRepository.state, settingsRepository.data) { tasbih, settings ->
                tasbih to settings.themeColor
            }.collect { (tasbih, themeColor) ->
                bubbleView?.updateContent(
                    count = tasbih.activeCount,
                    progress = tasbih.progressFraction,
                    dhikr = tasbih.activeDhikrText,
                    themeColor = themeColor,
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachBubbleOverlay() {
        val wm = windowManager ?: return
        val bubbleSizePx = dpToPx(82f)
        val dismissSizePx = dpToPx(64f)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - bubbleSizePx - dpToPx(16f)
            y = (screenHeight * 0.35f).toInt()
        }

        val dismissParams = WindowManager.LayoutParams(
            dismissSizePx,
            dismissSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dpToPx(44f)
        }

        val dismissView = DismissDropZoneView(this)
        dismissZoneView = dismissView
        dismissZoneParams = dismissParams

        val view = FloatingTasbihBubbleView(this).apply {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false
            val touchSlopPx = dpToPx(8f)

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (this.isCloseHit(event.x, event.y)) {
                            stopSelf()
                            return@setOnTouchListener true
                        }
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        hoveredDismiss = false
                        animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (!isDragging && (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx)) {
                            isDragging = true
                            showDismissZone()
                        }
                        if (isDragging) {
                            params.x = (initialX + dx).toInt().coerceIn(0, screenWidth - bubbleSizePx)
                            params.y = (initialY + dy).toInt().coerceIn(dpToPx(24f), screenHeight - bubbleSizePx)
                            runCatching { wm.updateViewLayout(this, params) }

                            val bubbleCenterX = params.x + bubbleSizePx / 2f
                            val bubbleCenterY = params.y + bubbleSizePx / 2f
                            val targetCenterX = screenWidth / 2f
                            val targetCenterY = screenHeight - dpToPx(44f) - dismissSizePx / 2f
                            val dist = hypot(bubbleCenterX - targetCenterX, bubbleCenterY - targetCenterY)
                            val insideDismiss = dist < dpToPx(64f)
                            if (insideDismiss != hoveredDismiss) {
                                hoveredDismiss = insideDismiss
                                dismissZoneView?.setHighlighted(insideDismiss)
                                if (insideDismiss) {
                                    TasbihHaptics.vibrateLight(this@FloatingTasbihService)
                                }
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                        hideDismissZone()
                        if (isDragging) {
                            if (hoveredDismiss) {
                                stopSelf()
                            } else {
                                // Snap horizontally to nearest screen edge for clean UX
                                params.x = if (params.x + bubbleSizePx / 2 < screenWidth / 2) {
                                    dpToPx(12f)
                                } else {
                                    screenWidth - bubbleSizePx - dpToPx(12f)
                                }
                                runCatching { wm.updateViewLayout(this, params) }
                            }
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            val outcome = tasbihRepository.increment()
                            val latest = tasbihRepository.state.value
                            TasbihHaptics.performFeedback(
                                context = this@FloatingTasbihService,
                                vibrationEnabled = latest.vibrationEnabled,
                                soundEnabled = latest.soundEnabled,
                                completedRound = outcome.completedRound,
                            )
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        bubbleView = view
        bubbleParams = params

        try {
            wm.addView(view, params)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun showDismissZone() {
        if (dismissZoneShown) return
        val wm = windowManager ?: return
        val view = dismissZoneView ?: return
        val params = dismissZoneParams ?: return
        runCatching {
            view.setHighlighted(false)
            wm.addView(view, params)
            dismissZoneShown = true
        }
    }

    private fun hideDismissZone() {
        if (!dismissZoneShown) return
        val wm = windowManager ?: return
        val view = dismissZoneView ?: return
        runCatching { wm.removeView(view) }
        dismissZoneShown = false
    }

    private fun removeOverlays() {
        val wm = windowManager ?: return
        bubbleView?.let { runCatching { wm.removeView(it) } }
        bubbleView = null
        hideDismissZone()
        dismissZoneView = null
    }

    private fun dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

    companion object {
        const val ACTION_START = "com.github.meypod.al_azan.action.START_FLOATING_TASBIH"
        const val ACTION_STOP = "com.github.meypod.al_azan.action.STOP_FLOATING_TASBIH"
        private const val CHANNEL_ID = "floating_tasbih_channel"
        private const val NOTIFICATION_ID = 49102

        fun start(context: Context) {
            val intent = Intent(context, FloatingTasbihService::class.java).apply { action = ACTION_START }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingTasbihService::class.java).apply { action = ACTION_STOP }
            context.stopService(intent)
        }
    }
}

private class FloatingTasbihBubbleView(context: Context) : View(context) {
    private var count: Int = 0
    private var progress: Float = 0f
    private var dhikrLabel: String = "سبحان الله"
    private var themeColor: ThemeColor = ThemeColor.ClassicLight

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val countTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val dhikrTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val closeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDC2626.toInt() }
    private val closeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val arcRect = RectF()

    fun isCloseHit(touchX: Float, touchY: Float): Boolean {
        val w = width.toFloat()
        val closeRadius = w * 0.14f
        val closeCx = w - closeRadius - 2f
        val closeCy = closeRadius + 2f
        return hypot(touchX - closeCx, touchY - closeCy) <= closeRadius * 1.5f
    }

    fun updateContent(
        count: Int,
        progress: Float,
        dhikr: String,
        themeColor: ThemeColor,
    ) {
        this.count = count
        this.progress = progress
        this.dhikrLabel = dhikr
        this.themeColor = themeColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val stroke = w * 0.085f
        val outerRadius = (minOf(w, h) / 2f) - 4f

        val (bgHex, primaryHex, trackHex, subTextHex) = when (themeColor) {
            ThemeColor.ClassicLight -> arrayOf(0xFFF6FAFA.toInt(), 0xFF00585A.toInt(), 0xFFD9E5E5.toInt(), 0xFF4C5E5E.toInt())
            ThemeColor.Dark -> arrayOf(0xFF141414.toInt(), 0xFF76D5D8.toInt(), 0xFF2B2B2B.toInt(), 0xFFD6DCDC.toInt())
            ThemeColor.MidnightBlue -> arrayOf(0xFF0F1B38.toInt(), 0xFFFFD166.toInt(), 0xFF24396B.toInt(), 0xFFE2E8F0.toInt())
        }

        bgPaint.color = bgHex
        canvas.drawCircle(cx, cy, outerRadius, bgPaint)

        shadowBorderPaint.color = trackHex
        shadowBorderPaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, outerRadius, shadowBorderPaint)

        val ringInset = stroke * 1.15f
        arcRect.set(ringInset, ringInset, w - ringInset, h - ringInset)

        trackPaint.color = trackHex
        trackPaint.strokeWidth = stroke
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

        progressPaint.color = primaryHex
        progressPaint.strokeWidth = stroke
        val sweep = (progress * 360f).coerceIn(0f, 360f)
        if (sweep > 0f) {
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)
        }

        countTextPaint.color = primaryHex
        countTextPaint.textSize = w * 0.28f
        canvas.drawText(count.toString(), cx, cy + (countTextPaint.textSize * 0.16f), countTextPaint)

        dhikrTextPaint.color = subTextHex
        dhikrTextPaint.textSize = w * 0.125f
        val shortLabel = if (dhikrLabel.length > 10) dhikrLabel.take(9) + "…" else dhikrLabel
        canvas.drawText(shortLabel, cx, cy + (w * 0.26f), dhikrTextPaint)

        // Quick Close button badge (×) at top-right corner
        val closeRadius = w * 0.14f
        val closeCx = w - closeRadius - 2f
        val closeCy = closeRadius + 2f
        canvas.drawCircle(closeCx, closeCy, closeRadius, closeBgPaint)
        closeTextPaint.textSize = closeRadius * 1.35f
        val closeBaseline = closeCy - ((closeTextPaint.descent() + closeTextPaint.ascent()) / 2f)
        canvas.drawText("✕", closeCx, closeBaseline, closeTextPaint)
    }
}

private class DismissDropZoneView(context: Context) : View(context) {
    private var highlighted = false

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun setHighlighted(active: Boolean) {
        highlighted = active
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) * if (highlighted) 0.96f else 0.84f
        circlePaint.color = if (highlighted) 0xFFDC2626.toInt() else 0xCC991B1B.toInt()
        canvas.drawCircle(cx, cy, radius, circlePaint)

        textPaint.textSize = width * 0.36f
        val baseline = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("✕", cx, baseline, textPaint)
    }
}
