package com.github.meypod.al_azan.intro.languageselection

import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

data class LanguageSelectionUiState(
    val selectedLocale: String = "en",
    val themeColor: ThemeColor = ThemeColor.Default,
)
