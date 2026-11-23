package com.github.meypod.al_azan.core.domain.util

import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A [city] found by [searchCities], with the alternative name that matched what the user typed. */
data class CityMatch(
    val city: CityGeoInfo,
    val matchedName: String,
)

/** Smallest query worth searching: one character would match tens of thousands of cities. */
const val CITY_SEARCH_MIN_QUERY_LENGTH = 2

private val ARABIC_ALEF_VARIANTS = "اأإآٱ"
private val ARABIC_YEH_VARIANTS = "يىئ"
private val ARABIC_HEH_VARIANTS = "هة"
private val ARABIC_WAW_VARIANTS = "وؤ"

private fun isArabicMark(ch: Char): Boolean = ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == '\u0640' // tashkeel, dagger alef, tatweel

/**
 * Folds letters that Arabic speakers type interchangeably (أ/إ/آ/ا, ى/ي, ة/ه, ؤ/و), drops diacritics and
 * lower-cases, so "ابو كبير" compares equal to "أبو كبير". Deliberately a plain char loop: it runs for
 * every candidate name of a search over ~140k cities.
 */
fun foldForCitySearch(input: String): String {
    val out = StringBuilder(input.length)
    for (raw in input) {
        if (isArabicMark(raw)) continue
        out.append(
            when (raw) {
                in ARABIC_ALEF_VARIANTS -> 'ا'
                in ARABIC_YEH_VARIANTS -> 'ي'
                in ARABIC_HEH_VARIANTS -> 'ه'
                in ARABIC_WAW_VARIANTS -> 'و'
                else -> raw.lowercaseChar()
            },
        )
    }
    return out.toString()
}

private fun arabicVariantClass(ch: Char): String? =
    when (ch) {
        in ARABIC_ALEF_VARIANTS -> "[$ARABIC_ALEF_VARIANTS]"
        in ARABIC_YEH_VARIANTS -> "[$ARABIC_YEH_VARIANTS]"
        in ARABIC_HEH_VARIANTS -> "[$ARABIC_HEH_VARIANTS]"
        in ARABIC_WAW_VARIANTS -> "[$ARABIC_WAW_VARIANTS]"
        else -> null
    }

/**
 * A regex matching [query] anywhere inside a raw comma-separated `names` string, tolerant of the Arabic
 * letter variants above and of space/hyphen/apostrophe differences. Null when the query is too short.
 * Used as a cheap pre-filter so only the (few) matching rows pay for the per-name scoring.
 */
fun buildCityQueryRegex(query: String): Regex? {
    val trimmed = query.trim()
    if (trimmed.length < CITY_SEARCH_MIN_QUERY_LENGTH) return null
    val pattern = StringBuilder()
    var lastWasSeparator = false
    for (ch in trimmed) {
        if (isArabicMark(ch)) continue
        if (ch.isWhitespace() || ch == '-' || ch == '\'' || ch == '’') {
            if (!lastWasSeparator) pattern.append("[\\s\\-'’]+")
            lastWasSeparator = true
            continue
        }
        lastWasSeparator = false
        pattern.append(arabicVariantClass(ch) ?: Regex.escape(ch.toString()))
    }
    if (pattern.isEmpty()) return null
    return Regex(pattern.toString(), RegexOption.IGNORE_CASE)
}

/**
 * Searches every city by any of its names (Arabic, English, ...). Best matches first: exact name, then
 * names starting with the query, then names containing it; shorter names win ties so "Tanta" ranks above
 * "Tanta Markaz Extension". At most [limit] results.
 */
fun List<CityGeoInfo>.searchCities(
    query: String,
    limit: Int = 15,
): List<CityMatch> {
    val regex = buildCityQueryRegex(query) ?: return emptyList()
    val needle = foldForCitySearch(query.trim())
    class Scored(
        val score: Int,
        val match: CityMatch,
    )

    val scored = ArrayList<Scored>()
    for (city in this) {
        if (!regex.containsMatchIn(city.names)) continue
        var bestScore = Int.MAX_VALUE
        var bestName = city.name
        for (alt in city.names.split(',')) {
            val name = alt.trim()
            if (name.isEmpty()) continue
            val index = foldForCitySearch(name).indexOf(needle)
            if (index < 0) continue
            val tier =
                when {
                    foldForCitySearch(name) == needle -> 0
                    index == 0 -> 1
                    else -> 2
                }
            val score = tier * 1000 + min(name.length, 999)
            if (score < bestScore) {
                bestScore = score
                bestName = name
            }
        }
        // The regex tolerates separators the folded comparison does not (e.g. "abu-kabir"); keep those
        // rows, just ranked below real matches.
        if (bestScore == Int.MAX_VALUE) bestScore = 3000 + min(city.name.length, 999)
        scored += Scored(bestScore, CityMatch(city, bestName))
    }
    return scored.sortedBy { it.score }.take(limit).map { it.match }
}

/** Great-circle distance in kilometres. */
fun distanceKm(
    lat1: Double,
    long1: Double,
    lat2: Double,
    long2: Double,
): Double {
    val earthRadiusKm = 6371.0088
    val dLat = Math.toRadians(lat2 - lat1)
    val dLong = Math.toRadians(long2 - long1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLong / 2).pow(2)
    return 2 * earthRadiusKm * asin(sqrt(a))
}

/**
 * The city closest to the given coordinates, or null when none lies within [maxKm]. Lets a GPS fix be
 * given a human name and a country without any network lookup.
 */
fun List<CityGeoInfo>.nearestTo(
    lat: Double,
    long: Double,
    maxKm: Double = 100.0,
): CityGeoInfo? {
    var best: CityGeoInfo? = null
    var bestKm = maxKm
    for (city in this) {
        // Cheap reject before the trig: one degree of latitude is ~111 km.
        if (kotlin.math.abs(city.lat - lat) > maxKm / 111.0) continue
        val km = distanceKm(lat, long, city.lat, city.long)
        if (km <= bestKm) {
            bestKm = km
            best = city
        }
    }
    return best
}

/**
 * Picks, from comma-separated alternative [names], the one to show for the app [language]: an Arabic-script
 * name for an Arabic UI when one exists, otherwise [fallback] (the Latin name).
 */
fun pickNameForLanguage(
    names: String,
    fallback: String,
    language: String,
): String {
    if (language != "ar") return fallback
    return names
        .split(',')
        .map { it.trim() }
        .firstOrNull { candidate -> candidate.any { it in '\u0600'..'\u06FF' } }
        ?: fallback
}

/** The city's name in the app language when it has one (Arabic UI → an Arabic-script name), else the Latin name. */
fun CityGeoInfo.displayNameFor(language: String): String = pickNameForLanguage(names, name, language)
