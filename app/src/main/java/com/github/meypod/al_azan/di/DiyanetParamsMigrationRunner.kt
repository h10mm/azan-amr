package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.domain.model.calculation.withDiyanetFixApplied
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import io.github.meypod.adhan_kotlin.CalculationMethod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time migration for users who selected [CalculationMethod.TURKEY] before adhan 0.0.13,
 * as well as one-time default updates for numbering system (Western Arabic) and persistent shade.
 */
@Singleton
class DiyanetParamsMigrationRunner
@Inject
constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun run() {
        if (!settingsRepository.fetch().defaultNumberingAndNotifMigrated) {
            settingsRepository.update {
                it.copy(
                    numberingSystem = NumberingSystem.Latn,
                    showWidget = false,
                    defaultNumberingAndNotifMigrated = true,
                )
            }
        }

        if (settingsRepository.fetch().diyanetFixApplied) return

        val current = calculationSettingsRepository.fetch()
        val fixed = current.withDiyanetFixApplied()
        // Corrected settings first, then the flag that says they are corrected: a failure in between
        // leaves the flag unset and this runs again, where the reverse would strand the old times.
        if (fixed != current) calculationSettingsRepository.update { fixed }
        settingsRepository.update {
            it.copy(
                diyanetFixApplied = true,
                // Nothing changed for a fresh install or a non-Diyanet user; there is nothing to explain.
                diyanetChangeNoticePending = it.diyanetChangeNoticePending || fixed != current,
            )
        }
    }
}
