package com.github.meypod.al_azan.intro.languageselection

import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

sealed interface LanguageSelectionUiAction {
    data class OnLanguageSelected(
        val locale: String,
    ) : LanguageSelectionUiAction

    data class OnThemeSelected(
        val themeColor: ThemeColor,
    ) : LanguageSelectionUiAction
}
