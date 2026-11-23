package com.github.meypod.al_azan.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.github.meypod.al_azan.R

object WisdomBitmapRenderer {

    @Volatile
    private var cachedKey: String? = null
    @Volatile
    private var cachedBitmap: Bitmap? = null

    @Synchronized
    fun renderWisdomBitmap(
        context: Context,
        rawText: String,
        widthDp: Int,
        textColor: Int,
        rowHeightDp: Int = 23,
        maxFontSp: Float = 17.0f,
        minFontSp: Float = 11.5f,
    ): Bitmap? {
        val trimChars = charArrayOf('«', '»', '"', '“', '”', '\'', '‘', '’', '•', '-', '—', ' ')
        val cleanText = rawText.trim { it in trimChars }
        if (cleanText.isBlank()) return null

        val density = context.resources.displayMetrics.density
        // Match the actual container width in dp so ImageView renders at 1:1 aspect ratio
        val targetWidthDp = widthDp.coerceIn(120, 600)
        val targetHeightDp = rowHeightDp.coerceIn(14, 38)
        val widthPx = (targetWidthDp * density).toInt()
        val heightPx = (targetHeightDp * density).toInt()

        val cacheKey = "$cleanText|$targetWidthDp|$targetHeightDp|$maxFontSp|$minFontSp|$textColor"
        val existing = cachedBitmap
        if (cachedKey == cacheKey && existing != null && !existing.isRecycled) {
            return existing
        }

        // 1. Load Authentic Naskh / Amiri Font locally from APK
        val typeface: Typeface = try {
            ResourcesCompat.getFont(context, R.font.amiri_regular)
                ?: Typeface.create("serif", Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.create("serif", Typeface.NORMAL)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            isSubpixelText = true
            isFilterBitmap = true
        }

        // Horizontal padding: 6dp total (3dp each side inside canvas)
        val horizontalPaddingPx = 6f * density
        val availableWidthPx = (widthPx - horizontalPaddingPx).coerceAtLeast(80f)

        // Ensure font size fits comfortably within vertical row height
        val maxAllowedByHeightSp = (targetHeightDp * 0.78f).coerceAtLeast(10.0f)
        val effectiveMaxSp = minOf(maxFontSp, maxAllowedByHeightSp)
        val effectiveMinSp = minOf(minFontSp, effectiveMaxSp)

        var currentSp = effectiveMaxSp
        paint.textSize = currentSp * density
        var measuredWidth = paint.measureText(cleanText)

        // 2. Smooth adaptive downscaling within single line
        while (currentSp > effectiveMinSp && measuredWidth > availableWidthPx) {
            currentSp -= 0.5f
            paint.textSize = currentSp * density
            measuredWidth = paint.measureText(cleanText)
        }

        var finalText = cleanText

        // 3. If still exceeding available width at minimum readable font, smartly truncate
        if (measuredWidth > availableWidthPx) {
            if (cleanText.contains('،')) {
                val firstClause = cleanText.substringBefore('،').trim { it in trimChars }
                if (firstClause.isNotBlank() && paint.measureText(firstClause) <= availableWidthPx) {
                    finalText = firstClause
                }
            }
            if (paint.measureText(finalText) > availableWidthPx) {
                var truncated = finalText
                while (truncated.length > 5 && paint.measureText("$truncated...") > availableWidthPx) {
                    truncated = truncated.dropLast(1).trimEnd()
                }
                finalText = "$truncated..."
            }
        }

        // 4. Render on Canvas with mathematically perfect centering
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fontMetrics = paint.fontMetrics
        val baselineY = (heightPx / 2f) - ((fontMetrics.ascent + fontMetrics.descent) / 2f)
        val centerX = widthPx / 2f

        canvas.drawText(finalText, centerX, baselineY, paint)

        cachedKey = cacheKey
        cachedBitmap = bitmap
        return bitmap
    }
}
