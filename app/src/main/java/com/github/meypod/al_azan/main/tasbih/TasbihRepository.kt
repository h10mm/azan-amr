package com.github.meypod.al_azan.main.tasbih

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the Electronic Tasbih state, backed by MMKV.
 * Shared seamlessly between [TasbihViewModel] and [FloatingTasbihService].
 */
@Singleton
class TasbihRepository @Inject constructor(
    private val mmkv: MMKV,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<TasbihState> = _state.asStateFlow()

    private val _isFloatingRunning = MutableStateFlow(false)
    val isFloatingRunning: StateFlow<Boolean> = _isFloatingRunning.asStateFlow()

    fun setFloatingRunning(running: Boolean) {
        _isFloatingRunning.value = running
    }

    fun increment(): TasbihIncrementOutcome {
        var completedRound = false
        var completedFullWird = false

        updateAndPersist { current ->
            if (current.isSequentialMode) {
                val stages = DEFAULT_SEQUENTIAL_WIRD
                val stageIndex = current.sequentialStepIndex.coerceIn(0, stages.lastIndex)
                val stage = stages[stageIndex]
                val nextCount = current.sequentialStepCount + 1
                if (nextCount >= stage.target) {
                    completedRound = true
                    val isLastStage = stageIndex == stages.lastIndex
                    completedFullWird = isLastStage
                    val nextStageIndex = if (isLastStage) 0 else stageIndex + 1
                    current.copy(
                        sequentialStepIndex = nextStageIndex,
                        sequentialStepCount = 0,
                        totalCount = current.totalCount + 1L,
                        cycleCount = if (isLastStage) current.cycleCount + 1 else current.cycleCount,
                    )
                } else {
                    current.copy(
                        sequentialStepCount = nextCount,
                        totalCount = current.totalCount + 1L,
                    )
                }
            } else {
                val selected = current.selectedItem
                val target = selected.target
                val nextCount = selected.count + 1
                val hitTarget = target > 0 && nextCount >= target
                if (hitTarget) {
                    completedRound = true
                }
                val finalCount = if (target > 0 && selected.count >= target) {
                    1
                } else if (target > 0 && nextCount >= target) {
                    target
                } else {
                    nextCount
                }
                val updatedItems = current.items.map { item ->
                    if (item.id == selected.id) item.copy(count = finalCount) else item
                }
                current.copy(
                    items = updatedItems,
                    totalCount = current.totalCount + 1L,
                    cycleCount = if (hitTarget) current.cycleCount + 1 else current.cycleCount,
                )
            }
        }

        return TasbihIncrementOutcome(
            completedRound = completedRound,
            completedFullSequentialWird = completedFullWird,
        )
    }

    fun selectItem(id: String) {
        updateAndPersist { current ->
            if (current.items.any { it.id == id }) {
                current.copy(
                    selectedId = id,
                    isSequentialMode = false,
                )
            } else {
                current
            }
        }
    }

    fun selectTargetType(type: ActiveTargetType) {
        updateAndPersist { current ->
            val targetVal = when (type) {
                ActiveTargetType.TARGET_33 -> 33
                ActiveTargetType.TARGET_100 -> 100
                ActiveTargetType.INFINITY -> 0
                ActiveTargetType.CUSTOM -> current.savedCustomTarget ?: 300
            }
            val selectedId = current.selectedItem.id
            current.copy(
                activeTargetType = type,
                isSequentialMode = false,
                items = current.items.map { item ->
                    if (item.id == selectedId) {
                        item.copy(
                            target = targetVal,
                            count = if (targetVal > 0 && item.count > targetVal) 0 else item.count,
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun setTarget(target: Int) {
        val safeTarget = target.coerceAtLeast(0)
        val targetType = when (safeTarget) {
            33 -> ActiveTargetType.TARGET_33
            100 -> ActiveTargetType.TARGET_100
            0 -> ActiveTargetType.INFINITY
            else -> ActiveTargetType.CUSTOM
        }
        updateAndPersist { current ->
            val selectedId = current.selectedItem.id
            current.copy(
                activeTargetType = targetType,
                isSequentialMode = false,
                items = current.items.map { item ->
                    if (item.id == selectedId) {
                        item.copy(
                            target = safeTarget,
                            count = if (safeTarget > 0 && item.count > safeTarget) 0 else item.count,
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun setSavedCustomTarget(target: Int) {
        val safeTarget = target.coerceAtLeast(1)
        updateAndPersist { current ->
            val selectedId = current.selectedItem.id
            current.copy(
                savedCustomTarget = safeTarget,
                activeTargetType = ActiveTargetType.CUSTOM,
                isSequentialMode = false,
                items = current.items.map { item ->
                    if (item.id == selectedId) {
                        item.copy(
                            target = safeTarget,
                            count = if (item.count > safeTarget) 0 else item.count,
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun addCustomItem(
        text: String,
        target: Int,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val newItem = TasbihItem(
            id = "custom_${UUID.randomUUID()}",
            text = trimmed,
            target = target.coerceAtLeast(0),
            count = 0,
            isCustom = true,
        )
        updateAndPersist { current ->
            current.copy(
                items = current.items + newItem,
                selectedId = newItem.id,
                isSequentialMode = false,
            )
        }
    }

    fun deleteCustomItem(id: String) {
        updateAndPersist { current ->
            val targetItem = current.items.firstOrNull { it.id == id }
            if (targetItem == null || !targetItem.isCustom) return@updateAndPersist current
            val remaining = current.items.filterNot { it.id == id }
            val nextSelected = if (current.selectedId == id) {
                remaining.firstOrNull()?.id ?: DEFAULT_TASBIH_ITEMS.first().id
            } else {
                current.selectedId
            }
            current.copy(
                items = remaining.ifEmpty { DEFAULT_TASBIH_ITEMS },
                selectedId = nextSelected,
            )
        }
    }

    fun toggleSequentialMode() {
        updateAndPersist { current ->
            val nextMode = !current.isSequentialMode
            current.copy(
                isSequentialMode = nextMode,
                sequentialStepIndex = if (nextMode) 0 else current.sequentialStepIndex,
                sequentialStepCount = if (nextMode) 0 else current.sequentialStepCount,
            )
        }
    }

    fun setSequentialMode(enabled: Boolean) {
        updateAndPersist { current ->
            current.copy(
                isSequentialMode = enabled,
                sequentialStepIndex = if (enabled && !current.isSequentialMode) 0 else current.sequentialStepIndex,
                sequentialStepCount = if (enabled && !current.isSequentialMode) 0 else current.sequentialStepCount,
            )
        }
    }

    fun cycleSensoryMode(): SensoryFeedbackMode {
        var resultMode = SensoryFeedbackMode.VIBRATE_ONLY
        updateAndPersist { current ->
            val nextMode = when (current.sensoryMode) {
                SensoryFeedbackMode.VIBRATE_ONLY -> SensoryFeedbackMode.SOUND_AND_VIBRATE
                SensoryFeedbackMode.SOUND_AND_VIBRATE -> SensoryFeedbackMode.SILENT
                SensoryFeedbackMode.SILENT -> SensoryFeedbackMode.VIBRATE_ONLY
            }
            resultMode = nextMode
            when (nextMode) {
                SensoryFeedbackMode.VIBRATE_ONLY -> current.copy(vibrationEnabled = true, soundEnabled = false)
                SensoryFeedbackMode.SOUND_AND_VIBRATE -> current.copy(vibrationEnabled = true, soundEnabled = true)
                SensoryFeedbackMode.SILENT -> current.copy(vibrationEnabled = false, soundEnabled = false)
            }
        }
        return resultMode
    }

    fun toggleVibration() {
        updateAndPersist { it.copy(vibrationEnabled = !it.vibrationEnabled) }
    }

    fun toggleSound() {
        updateAndPersist { it.copy(soundEnabled = !it.soundEnabled) }
    }

    fun toggleVolumeKeys() {
        updateAndPersist { it.copy(volumeKeysEnabled = !it.volumeKeysEnabled) }
    }

    fun setVolumeKeys(enabled: Boolean) {
        updateAndPersist { it.copy(volumeKeysEnabled = enabled) }
    }

    fun resetCurrent() {
        updateAndPersist { current ->
            if (current.isSequentialMode) {
                current.copy(sequentialStepIndex = 0, sequentialStepCount = 0, cycleCount = 1)
            } else {
                val selectedId = current.selectedItem.id
                current.copy(
                    items = current.items.map { item ->
                        if (item.id == selectedId) item.copy(count = 0) else item
                    },
                    cycleCount = 1,
                )
            }
        }
    }

    fun resetAll() {
        updateAndPersist { current ->
            current.copy(
                items = current.items.map { it.copy(count = 0) },
                totalCount = 0L,
                sequentialStepIndex = 0,
                sequentialStepCount = 0,
                cycleCount = 1,
            )
        }
    }

    fun resetTotal() {
        updateAndPersist { current ->
            current.copy(totalCount = 0L)
        }
    }

    private fun updateAndPersist(transform: (TasbihState) -> TasbihState) {
        var updatedState: TasbihState? = null
        _state.update { current ->
            transform(current).also { updatedState = it }
        }
        updatedState?.let { stateToSave ->
            runCatching {
                mmkv.encode(KEY_TASBIH_STATE, json.encodeToString(TasbihState.serializer(), stateToSave))
            }
        }
    }

    private fun loadState(): TasbihState {
        val rawV4 = mmkv.decodeString(KEY_TASBIH_STATE, null)
        if (rawV4 != null) {
            return runCatching {
                val decoded = json.decodeFromString(TasbihState.serializer(), rawV4)
                var sanitized = if (decoded.items.isEmpty()) decoded.copy(items = DEFAULT_TASBIH_ITEMS) else decoded
                if (sanitized.savedCustomTarget == 33 || sanitized.savedCustomTarget == 0) {
                    sanitized = sanitized.copy(savedCustomTarget = null)
                }
                sanitized
            }.getOrDefault(TasbihState())
        }

        // Migration from previous state version: apply new defaults
        val rawV3 = mmkv.decodeString("TASBIH_STATE_V3", null)
        if (rawV3 != null) {
            val migrated = runCatching {
                val decoded = json.decodeFromString(TasbihState.serializer(), rawV3)
                decoded.copy(
                    isSequentialMode = true,
                    vibrationEnabled = true,
                    soundEnabled = true,
                    savedCustomTarget = if (decoded.savedCustomTarget == 33 || decoded.savedCustomTarget == 0) null else decoded.savedCustomTarget,
                )
            }.getOrNull()
            if (migrated != null) {
                runCatching {
                    mmkv.encode(KEY_TASBIH_STATE, json.encodeToString(TasbihState.serializer(), migrated))
                }
                return migrated
            }
        }

        val defaultState = TasbihState()
        runCatching {
            mmkv.encode(KEY_TASBIH_STATE, json.encodeToString(TasbihState.serializer(), defaultState))
        }
        return defaultState
    }

    private companion object {
        const val KEY_TASBIH_STATE = "TASBIH_STATE_V4"
    }
}
