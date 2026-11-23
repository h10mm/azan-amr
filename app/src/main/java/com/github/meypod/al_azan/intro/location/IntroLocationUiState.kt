package com.github.meypod.al_azan.intro.location

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.util.CityMatch

/** One row of the quick city search: the matched city and the country it belongs to (when known). */
@Immutable
data class IntroCityResult(
    val match: CityMatch,
    val country: CountryGeoInfo?,
)

@Immutable
data class IntroLocationUiState(
    val query: String = "",
    val results: List<IntroCityResult> = emptyList(),
    val searching: Boolean = false,
    /** True once a search ran to completion for a long-enough query — distinguishes "no results" from "not searched yet". */
    val searched: Boolean = false,
    val resolvingGps: Boolean = false,
    /** The location currently used for the calculation, if any. */
    val selectedLocation: FavoriteLocation? = null,
)
