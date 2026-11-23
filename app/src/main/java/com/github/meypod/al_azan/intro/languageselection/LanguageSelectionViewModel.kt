package com.github.meypod.al_azan.intro.languageselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.ChangeLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val changeLanguageUseCase: ChangeLanguageUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LanguageSelectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settingsRepository.fetch()
            _uiState.update {
                it.copy(
                    selectedLocale = current.selectedLocale,
                    themeColor = current.themeColor,
                )
            }
            delay(300) // wait for language sync to finish (just a little insurance)
            settingsRepository.update {
                it.copy(selectedArabicCalendar = if (it.selectedLocale.startsWith("fa")) "islamic-civil" else "islamic")
            }
        }
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update {
                    it.copy(
                        selectedLocale = settings.selectedLocale,
                        themeColor = settings.themeColor,
                    )
                }
            }
        }
    }

    fun onAction(action: LanguageSelectionUiAction) {
        when (action) {
            is LanguageSelectionUiAction.OnLanguageSelected -> onLanguageSelected(action.locale)
            is LanguageSelectionUiAction.OnThemeSelected -> onThemeSelected(action.themeColor)
        }
    }

    private fun onLanguageSelected(locale: String) {
        viewModelScope.launch {
            changeLanguageUseCase(locale)
        }
        _uiState.update {
            it.copy(selectedLocale = locale)
        }
    }

    private fun onThemeSelected(themeColor: ThemeColor) {
        _uiState.update {
            it.copy(themeColor = themeColor)
        }
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(themeColor = themeColor)
            }
        }
    }
}
