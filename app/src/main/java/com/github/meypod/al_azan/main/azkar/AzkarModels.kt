package com.github.meypod.al_azan.main.azkar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.main.tasbih.TasbihSpeechController

@Immutable
enum class AzkarCategoryId(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val isDailyShortcut: Boolean = false,
) {
    MORNING(
        id = "morning",
        titleRes = R.string.azkar_cat_morning,
        iconRes = R.drawable.ic_sun,
        isDailyShortcut = true,
    ),
    EVENING(
        id = "evening",
        titleRes = R.string.azkar_cat_evening,
        iconRes = R.drawable.ic_moon,
        isDailyShortcut = true,
    ),
    SLEEP(
        id = "sleep",
        titleRes = R.string.azkar_cat_sleep,
        iconRes = R.drawable.ic_moon,
        isDailyShortcut = true,
    ),
    PRAYER(
        id = "prayer",
        titleRes = R.string.azkar_cat_prayer,
        iconRes = R.drawable.mosque,
        isDailyShortcut = true,
    ),
    WAKING(
        id = "waking",
        titleRes = R.string.azkar_cat_waking,
        iconRes = R.drawable.ic_sun,
    ),
    HOME_MOSQUE(
        id = "home_mosque",
        titleRes = R.string.azkar_cat_home_mosque,
        iconRes = R.drawable.ic_home_outline,
    ),
    TRAVEL(
        id = "travel",
        titleRes = R.string.azkar_cat_travel,
        iconRes = R.drawable.ic_travel,
    ),
    DISTRESS(
        id = "distress",
        titleRes = R.string.azkar_cat_distress,
        iconRes = R.drawable.ic_shield_heart,
    ),
    ISTIGHFAR(
        id = "istighfar",
        titleRes = R.string.azkar_cat_istighfar,
        iconRes = R.drawable.ic_sparkles,
    ),
    FOOD(
        id = "food",
        titleRes = R.string.azkar_cat_food,
        iconRes = R.drawable.ic_restaurant,
    ),
    ;

    companion object {
        fun fromId(id: String): AzkarCategoryId =
            entries.firstOrNull { it.id == id } ?: MORNING
    }
}

@Immutable
data class AzkarItem(
    val id: String,
    val category: AzkarCategoryId,
    val arabicText: String,
    val englishTranslation: String,
    val repeatCount: Int,
    val source: String,
    val benefit: String,
) {
    val normalizedArabic: String by lazy {
        TasbihSpeechController.normalizeArabic(arabicText)
    }

    val normalizedSource: String by lazy {
        TasbihSpeechController.normalizeArabic(source)
    }
}

@Immutable
data class AzkarCategory(
    val id: AzkarCategoryId,
    val items: List<AzkarItem>,
)
