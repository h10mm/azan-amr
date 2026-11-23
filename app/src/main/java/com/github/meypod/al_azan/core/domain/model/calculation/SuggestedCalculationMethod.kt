package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import java.util.Locale

/**
 * Canonical calculation parameters for each method.
 * For the Egyptian General Authority of Survey (الهيئة المصرية العامة للمساحة), official prayer tables,
 * Dar Al-Ifta, and Egyptian State Radio calculate Dhuhr at exact solar noon (meridian transit) without
 * adding an arbitrary 1-minute buffer. batoulapps/adhan hardcodes `methodAdjustments.dhuhr = 1`, which puts
 * Dhuhr 1 minute ahead of official Egyptian calendars throughout the year.
 */
fun CalculationMethod.canonicalParameters(): CalculationParameters {
    val base = this.parameters
    return if (this == CalculationMethod.EGYPTIAN) {
        base.copy(methodAdjustments = base.methodAdjustments.copy(dhuhr = 0))
    } else {
        base
    }
}

/**
 * Ensures any stored Egyptian calculation method has the artificial +1 minute Dhuhr buffer cleared.
 */
fun CalculationSettings.withEgyptianFixApplied(): CalculationSettings {
    val current = parameters ?: return this
    if (current.method != CalculationMethod.EGYPTIAN) return this
    if (current.methodAdjustments.dhuhr == 0) return this
    return copy(
        parameters = current.copy(
            methodAdjustments = current.methodAdjustments.copy(dhuhr = 0),
        ),
    )
}

/**
 * The calculation method most commonly used by the official prayer-time authority of a country
 * (ISO 3166-1 alpha-2 [countryCode], case-insensitive).
 *
 * It is only a starting point for onboarding — the user can always change it — so countries without a
 * well-established national authority in the list fall back to [CalculationMethod.MUSLIM_WORLD_LEAGUE],
 * the neutral global default, rather than guessing.
 */
fun suggestedCalculationMethod(countryCode: String?): CalculationMethod =
    when (countryCode?.trim()?.uppercase(Locale.ROOT)) {
        "EG" -> CalculationMethod.EGYPTIAN
        "SA" -> CalculationMethod.UMM_AL_QURA
        "AE" -> CalculationMethod.DUBAI
        "KW" -> CalculationMethod.KUWAIT
        "QA" -> CalculationMethod.QATAR
        "BH", "OM" -> CalculationMethod.GULF
        "TR" -> CalculationMethod.TURKEY
        "DZ" -> CalculationMethod.ALGERIA
        "TN" -> CalculationMethod.TUNISIA
        "MA" -> CalculationMethod.MOROCCO
        "SG", "MY" -> CalculationMethod.SINGAPORE
        "ID" -> CalculationMethod.KEMENAG
        "BN" -> CalculationMethod.BRUNEI
        "FR" -> CalculationMethod.FRANCE
        "RU" -> CalculationMethod.RUSSIA
        "IR" -> CalculationMethod.TEHRAN
        "PK", "IN", "BD", "AF" -> CalculationMethod.KARACHI
        "US", "CA" -> CalculationMethod.NORTH_AMERICA
        else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
    }
