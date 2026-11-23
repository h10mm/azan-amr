package com.github.meypod.al_azan.widget

import android.content.Context
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.main.azkar.AzkarCategoryId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tracks dismissed spiritual pill moments and already-fired reminders for today so that:
 * 1. Once a user swipes away a spiritual pill notification, periodic 5-minute widget refreshes NEVER re-post it.
 * 2. When a scheduled Reminder fires for a topic (e.g., Duha, Kahf, Mulk, Azkar, Fasting),
 *    the companion spiritual pill does not duplicate that topic before or after the reminder is dismissed.
 */
object SpiritualPillDismissStore {

    private const val PREFS_NAME = "spiritual_pill_dismiss_prefs"
    private const val KEY_DATE = "last_date"
    private const val KEY_DISMISSED_SET = "dismissed_keys"

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun currentSet(context: Context): Set<String> {
        val p = prefs(context)
        val today = todayKey()
        if (p.getString(KEY_DATE, null) != today) {
            p.edit().putString(KEY_DATE, today).putStringSet(KEY_DISMISSED_SET, emptySet()).apply()
            return emptySet()
        }
        return p.getStringSet(KEY_DISMISSED_SET, emptySet())?.toSet() ?: emptySet()
    }

    fun markDismissed(context: Context, key: String) {
        if (key.isBlank()) return
        val existing = currentSet(context)
        if (key !in existing) {
            prefs(context).edit()
                .putString(KEY_DATE, todayKey())
                .putStringSet(KEY_DISMISSED_SET, existing + key)
                .apply()
        }
    }

    fun isDismissed(context: Context, key: String): Boolean {
        if (key.isBlank()) return false
        return currentSet(context).contains(key)
    }

    /** Normalized topic key for a [SpiritualContext] moment. */
    fun momentKeyForContext(info: SpiritualContext): String {
        val combined = "${info.wordTag} ${info.title}"
        return when {
            combined.contains("الكهف") || combined.contains("كَهْف") -> "topic_kahf"
            combined.contains("الملك") || combined.contains("مُلْك") -> "topic_mulk"
            combined.contains("الضحى") || combined.contains("ضُحَى") -> "topic_duha"
            combined.contains("وتر") || combined.contains("وِتْر") || combined.contains("قيام") || combined.contains("قِيَام") -> "topic_qiyam"
            combined.contains("صيام") || combined.contains("صِيَام") || combined.contains("الأيام البيض") -> "topic_fasting"
            combined.contains("الصباح") -> "topic_azkar_morning"
            combined.contains("المساء") -> "topic_azkar_evening"
            combined.contains("النوم") -> "topic_azkar_sleep"
            combined.contains("ساعة الاستجابة") || combined.contains("سَاعَة إِجَابَة") -> "topic_friday_istijabah"
            else -> "moment_${info.title.trim()}"
        }
    }

    /** Marks the matching spiritual pill topic as already delivered when a scheduled Reminder fires. */
    fun markDismissedForReminder(context: Context, reminderId: String, title: String) {
        val combined = "$reminderId $title"
        val key = when {
            combined.contains("kahf", ignoreCase = true) || combined.contains("الكهف") -> "topic_kahf"
            combined.contains("mulk", ignoreCase = true) || combined.contains("الملك") -> "topic_mulk"
            combined.contains("duha", ignoreCase = true) || combined.contains("الضحى") -> "topic_duha"
            combined.contains("qiyam", ignoreCase = true) || combined.contains("قيام") || combined.contains("وتر") -> "topic_qiyam"
            combined.contains("fasting", ignoreCase = true) || combined.contains("صيام") -> "topic_fasting"
            combined.contains("morning", ignoreCase = true) || combined.contains("الصباح") -> "topic_azkar_morning"
            combined.contains("evening", ignoreCase = true) || combined.contains("المساء") -> "topic_azkar_evening"
            combined.contains("sleep", ignoreCase = true) || combined.contains("النوم") -> "topic_azkar_sleep"
            combined.contains("istijabah", ignoreCase = true) || combined.contains("إجابة") -> "topic_friday_istijabah"
            else -> null
        }
        if (key != null) markDismissed(context, key)
    }

    /** Resolves the exact Azkar category route for a notification so tapping opens that exact dhikr category. */
    fun resolveAzkarRoute(idOrTag: String, title: String, subtitle: String = ""): DeepLinkableRoute {
        val combined = "$idOrTag $title $subtitle"
        val category: AzkarCategoryId? = when {
            combined.contains("morning", ignoreCase = true) || combined.contains("الصباح") -> AzkarCategoryId.MORNING
            combined.contains("evening", ignoreCase = true) || combined.contains("المساء") -> AzkarCategoryId.EVENING
            combined.contains("sleep", ignoreCase = true) || combined.contains("النوم") -> AzkarCategoryId.SLEEP
            combined.contains("waking", ignoreCase = true) || combined.contains("الاستيقاظ") -> AzkarCategoryId.WAKING
            combined.contains("معقبات") || combined.contains("بعد الصلاة") || combined.contains("أذان وإقامة") || combined.contains("الفريضة") -> AzkarCategoryId.PRAYER
            combined.contains("استغفار") || combined.contains("الاستغفار") || combined.contains("النهي") -> AzkarCategoryId.ISTIGHFAR
            combined.contains("إجابة") || combined.contains("إِجَابَة") || combined.contains("دعاء") || combined.contains("دُعَاء") -> AzkarCategoryId.DISTRESS
            else -> null
        }
        return if (category != null) {
            Route.Main.AzkarDetail(categoryId = category.id)
        } else {
            Route.Main.Azkar
        }
    }
}
