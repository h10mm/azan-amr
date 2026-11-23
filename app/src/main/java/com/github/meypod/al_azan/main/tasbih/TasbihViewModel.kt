package com.github.meypod.al_azan.main.tasbih

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class TasbihUiState(
    val tasbih: TasbihState = TasbihState(),
    val isVoiceListening: Boolean = false,
    val isFloatingRunning: Boolean = false,
    val showDhikrPickerSheet: Boolean = false,
    val showCustomTargetDialog: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showOverlayPermissionDialog: Boolean = false,
)

sealed interface TasbihUiAction {
    data object OnIncrement : TasbihUiAction
    data class OnSelectItem(val id: String) : TasbihUiAction
    data class OnSetTarget(val target: Int) : TasbihUiAction
    data class OnSelectTargetType(val type: ActiveTargetType) : TasbihUiAction
    data class OnSaveCustomTarget(val target: Int) : TasbihUiAction
    data class OnAddCustomItem(val text: String, val target: Int) : TasbihUiAction
    data class OnDeleteCustomItem(val id: String) : TasbihUiAction
    data object OnToggleSequentialMode : TasbihUiAction
    data class OnSetSequentialMode(val enabled: Boolean) : TasbihUiAction
    data object OnCycleSensoryMode : TasbihUiAction
    data object OnToggleVolumeKeys : TasbihUiAction
    data class OnSetVolumeKeys(val enabled: Boolean) : TasbihUiAction
    data class OnVoiceListeningChanged(val listening: Boolean) : TasbihUiAction
    data object OnOpenDhikrPicker : TasbihUiAction
    data object OnDismissDhikrPicker : TasbihUiAction
    data object OnOpenCustomTargetDialog : TasbihUiAction
    data object OnDismissCustomTargetDialog : TasbihUiAction
    data object OnOpenResetDialog : TasbihUiAction
    data object OnDismissResetDialog : TasbihUiAction
    data object OnConfirmResetCurrent : TasbihUiAction
    data object OnConfirmResetAll : TasbihUiAction
    data object OnResetTotal : TasbihUiAction
    data object OnOpenSettingsSheet : TasbihUiAction
    data object OnDismissSettingsSheet : TasbihUiAction
    data object OnOpenOverlayPermissionDialog : TasbihUiAction
    data object OnDismissOverlayPermissionDialog : TasbihUiAction
}

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val tasbihRepository: TasbihRepository,
) : ViewModel() {

    private data class EphemeralUiState(
        val isVoiceListening: Boolean = false,
        val showDhikrPickerSheet: Boolean = false,
        val showCustomTargetDialog: Boolean = false,
        val showResetConfirmDialog: Boolean = false,
        val showSettingsSheet: Boolean = false,
        val showOverlayPermissionDialog: Boolean = false,
    )

    private val ephemeral = MutableStateFlow(EphemeralUiState())

    val uiState: StateFlow<TasbihUiState> = combine(
        tasbihRepository.state,
        tasbihRepository.isFloatingRunning,
        ephemeral,
    ) { tasbih, floatingRunning, ui ->
        TasbihUiState(
            tasbih = tasbih,
            isVoiceListening = ui.isVoiceListening,
            isFloatingRunning = floatingRunning,
            showDhikrPickerSheet = ui.showDhikrPickerSheet,
            showCustomTargetDialog = ui.showCustomTargetDialog,
            showResetConfirmDialog = ui.showResetConfirmDialog,
            showSettingsSheet = ui.showSettingsSheet,
            showOverlayPermissionDialog = ui.showOverlayPermissionDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TasbihUiState(
            tasbih = tasbihRepository.state.value,
            isFloatingRunning = tasbihRepository.isFloatingRunning.value,
        ),
    )

    fun incrementAndReport(): TasbihIncrementOutcome = tasbihRepository.increment()

    fun cycleSensoryModeAndReport(): SensoryFeedbackMode = tasbihRepository.cycleSensoryMode()

    fun onAction(action: TasbihUiAction) {
        when (action) {
            TasbihUiAction.OnIncrement -> tasbihRepository.increment()
            is TasbihUiAction.OnSelectItem -> tasbihRepository.selectItem(action.id)
            is TasbihUiAction.OnSetTarget -> tasbihRepository.setTarget(action.target)
            is TasbihUiAction.OnSelectTargetType -> tasbihRepository.selectTargetType(action.type)
            is TasbihUiAction.OnSaveCustomTarget -> {
                tasbihRepository.setSavedCustomTarget(action.target)
                ephemeral.update { it.copy(showCustomTargetDialog = false) }
            }
            is TasbihUiAction.OnAddCustomItem -> tasbihRepository.addCustomItem(action.text, action.target)
            is TasbihUiAction.OnDeleteCustomItem -> tasbihRepository.deleteCustomItem(action.id)
            TasbihUiAction.OnToggleSequentialMode -> tasbihRepository.toggleSequentialMode()
            is TasbihUiAction.OnSetSequentialMode -> tasbihRepository.setSequentialMode(action.enabled)
            TasbihUiAction.OnCycleSensoryMode -> tasbihRepository.cycleSensoryMode()
            TasbihUiAction.OnToggleVolumeKeys -> tasbihRepository.toggleVolumeKeys()
            is TasbihUiAction.OnSetVolumeKeys -> tasbihRepository.setVolumeKeys(action.enabled)
            is TasbihUiAction.OnVoiceListeningChanged -> ephemeral.update { it.copy(isVoiceListening = action.listening) }
            TasbihUiAction.OnOpenDhikrPicker -> ephemeral.update { it.copy(showDhikrPickerSheet = true) }
            TasbihUiAction.OnDismissDhikrPicker -> ephemeral.update { it.copy(showDhikrPickerSheet = false) }
            TasbihUiAction.OnOpenCustomTargetDialog -> ephemeral.update { it.copy(showCustomTargetDialog = true) }
            TasbihUiAction.OnDismissCustomTargetDialog -> ephemeral.update { it.copy(showCustomTargetDialog = false) }
            TasbihUiAction.OnOpenResetDialog -> ephemeral.update { it.copy(showResetConfirmDialog = true) }
            TasbihUiAction.OnDismissResetDialog -> ephemeral.update { it.copy(showResetConfirmDialog = false) }
            TasbihUiAction.OnConfirmResetCurrent -> {
                tasbihRepository.resetCurrent()
                ephemeral.update { it.copy(showResetConfirmDialog = false) }
            }
            TasbihUiAction.OnConfirmResetAll -> {
                tasbihRepository.resetAll()
                ephemeral.update { it.copy(showResetConfirmDialog = false) }
            }
            TasbihUiAction.OnResetTotal -> tasbihRepository.resetTotal()
            TasbihUiAction.OnOpenSettingsSheet -> ephemeral.update { it.copy(showSettingsSheet = true) }
            TasbihUiAction.OnDismissSettingsSheet -> ephemeral.update { it.copy(showSettingsSheet = false) }
            TasbihUiAction.OnOpenOverlayPermissionDialog -> ephemeral.update { it.copy(showOverlayPermissionDialog = true) }
            TasbihUiAction.OnDismissOverlayPermissionDialog -> ephemeral.update { it.copy(showOverlayPermissionDialog = false) }
        }
    }
}
