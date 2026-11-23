package com.github.meypod.al_azan.main.tasbih

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TasbihItem(
    val id: String,
    val text: String,
    val target: Int = 33, // 0 means Free / Open (∞)
    val count: Int = 0,
    val isCustom: Boolean = false,
)

@Immutable
@Serializable
data class SequentialWirdStage(
    val text: String,
    val target: Int,
)

val DEFAULT_SEQUENTIAL_WIRD: List<SequentialWirdStage> = listOf(
    SequentialWirdStage(text = "سبحان الله", target = 33),
    SequentialWirdStage(text = "الحمد لله", target = 33),
    SequentialWirdStage(text = "الله أكبر", target = 34),
)

val DEFAULT_TASBIH_ITEMS: List<TasbihItem> = listOf(
    TasbihItem(id = "subhan_allah", text = "سبحان الله", target = 33),
    TasbihItem(id = "alhamdulillah", text = "الحمد لله", target = 33),
    TasbihItem(id = "allahu_akbar", text = "الله أكبر", target = 34),
    TasbihItem(id = "astaghfirullah", text = "أستغفر الله", target = 100),
    TasbihItem(id = "la_ilaha_illallah", text = "لا إله إلا الله", target = 100),
    TasbihItem(id = "salawat", text = "اللهم صلِّ على محمد", target = 100),
    TasbihItem(id = "subhan_allah_wabihamdihi", text = "سبحان الله وبحمده", target = 100),
)

@Serializable
enum class SensoryFeedbackMode {
    VIBRATE_ONLY,
    SOUND_AND_VIBRATE,
    SILENT,
}

@Serializable
enum class ActiveTargetType {
    TARGET_33,
    TARGET_100,
    INFINITY,
    CUSTOM,
}

@Immutable
@Serializable
data class TasbihState(
    val items: List<TasbihItem> = DEFAULT_TASBIH_ITEMS,
    val selectedId: String = DEFAULT_TASBIH_ITEMS.first().id,
    val totalCount: Long = 0L,
    val isSequentialMode: Boolean = true,
    val sequentialStepIndex: Int = 0,
    val sequentialStepCount: Int = 0,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val savedCustomTarget: Int? = null,
    val activeTargetType: ActiveTargetType = ActiveTargetType.TARGET_33,
    val cycleCount: Int = 1,
    val volumeKeysEnabled: Boolean = true,
) {
    val selectedItem: TasbihItem
        get() = items.firstOrNull { it.id == selectedId } ?: items.firstOrNull() ?: DEFAULT_TASBIH_ITEMS.first()

    val activeDhikrText: String
        get() = if (isSequentialMode) {
            DEFAULT_SEQUENTIAL_WIRD.getOrElse(sequentialStepIndex) { DEFAULT_SEQUENTIAL_WIRD.first() }.text
        } else {
            selectedItem.text
        }

    val activeTarget: Int
        get() = if (isSequentialMode) {
            DEFAULT_SEQUENTIAL_WIRD.getOrElse(sequentialStepIndex) { DEFAULT_SEQUENTIAL_WIRD.first() }.target
        } else {
            selectedItem.target
        }

    val activeCount: Int
        get() = if (isSequentialMode) {
            sequentialStepCount
        } else {
            selectedItem.count
        }

    val progressFraction: Float
        get() {
            val target = activeTarget
            if (target <= 0) return 0f
            return (activeCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }

    val sensoryMode: SensoryFeedbackMode
        get() = when {
            vibrationEnabled && soundEnabled -> SensoryFeedbackMode.SOUND_AND_VIBRATE
            vibrationEnabled && !soundEnabled -> SensoryFeedbackMode.VIBRATE_ONLY
            else -> SensoryFeedbackMode.SILENT
        }
}

@Immutable
data class TasbihIncrementOutcome(
    val completedRound: Boolean,
    val completedFullSequentialWird: Boolean = false,
)
