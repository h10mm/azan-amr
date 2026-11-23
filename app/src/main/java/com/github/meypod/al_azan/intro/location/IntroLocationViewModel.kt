package com.github.meypod.al_azan.intro.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.canonicalParameters
import com.github.meypod.al_azan.core.domain.model.calculation.suggestedCalculationMethod
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.GeoInfoRepository
import com.github.meypod.al_azan.core.domain.util.CITY_SEARCH_MIN_QUERY_LENGTH
import com.github.meypod.al_azan.core.domain.util.CityMatch
import com.github.meypod.al_azan.core.domain.util.displayNameFor
import com.github.meypod.al_azan.core.domain.util.distanceKm
import com.github.meypod.al_azan.core.domain.util.nearestTo
import com.github.meypod.al_azan.core.domain.util.searchCities
import com.github.meypod.al_azan.main.location.components.NewLocationDialogUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A GPS fix is only given the name of the nearest city when that city is genuinely close. */
private const val GPS_LABEL_MAX_KM = 30.0

/** How far from the fix a city may be and still lend its country (and so its calculation method). */
private const val GPS_COUNTRY_MAX_KM = 150.0

private const val SEARCH_DEBOUNCE_MS = 250L

@HiltViewModel
class IntroLocationViewModel
@Inject constructor(
    private val geoInfoRepository: GeoInfoRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IntroLocationUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Locations created during this onboarding session; a newer pick replaces them instead of piling up. */
    private val addedLocationIds = mutableSetOf<String>()

    private var suggestionInFlight = false

    init {
        viewModelScope.launch {
            combine(
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) { calc, locations ->
                locations.firstOrNull { it.id == calc.locationId } to calc.parameters
            }.collect { (selected, parameters) ->
                _uiState.update { it.copy(selectedLocation = selected) }
                if (selected != null && parameters == null) suggestMethodFor(selected.locationDetail)
            }
        }
    }

    suspend fun getCountries() = geoInfoRepository.getCountries()

    suspend fun getCities(countryCode: String) = geoInfoRepository.getCities(countryCode)

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.trim().length < CITY_SEARCH_MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(results = emptyList(), searching = false, searched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(searching = true) }
            delay(SEARCH_DEBOUNCE_MS)
            val countries = geoInfoRepository.getCountries().associateBy { it.code }
            val matches = withContext(Dispatchers.Default) { geoInfoRepository.getCities().searchCities(query) }
            _uiState.update { state ->
                state.copy(
                    results = matches.map { IntroCityResult(it, countries[it.city.country]) },
                    searching = false,
                    searched = true,
                )
            }
        }
    }

    /**
     * One-tap quick city selection: resolves the matching city from the geo database and
     * immediately sets it as the active location and updates the country's calculation method.
     */
    fun onQuickCitySelected(
        cityName: String,
        language: String,
    ) {
        viewModelScope.launch {
            val matches = withContext(Dispatchers.Default) {
                geoInfoRepository.getCities().searchCities(cityName)
            }
            val bestMatch = matches.firstOrNull()
            if (bestMatch != null) {
                onCitySelected(bestMatch, language)
            } else {
                onQueryChange(cityName)
            }
        }
    }

    fun onCitySelected(
        match: CityMatch,
        language: String,
    ) {
        viewModelScope.launch {
            val city = match.city
            val country = geoInfoRepository.getCountries().firstOrNull { it.code == city.country }
            val shownName = if (language == "ar") city.displayNameFor(language) else match.matchedName
            selectLocation(
                CalculationLocationDetail(
                    lat = city.lat,
                    long = city.long,
                    city = city.copy(selectedName = match.matchedName),
                    country = country,
                    label = shownName,
                ),
            )
            searchJob?.cancel()
            _uiState.update { it.copy(query = "", results = emptyList(), searching = false, searched = false) }
        }
    }

    /** Result of the system location prompt/fix (see `rememberLocationAccessHelperDialogs`). */
    fun onGpsLocation(
        fix: CalculationLocationDetail,
        language: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(resolvingGps = true) }
            try {
                val nearest = withContext(Dispatchers.Default) {
                    geoInfoRepository.getCities().nearestTo(fix.lat, fix.long, GPS_COUNTRY_MAX_KM)
                }
                val country = nearest?.let { near -> geoInfoRepository.getCountries().firstOrNull { it.code == near.country } }
                val label = nearest
                    ?.takeIf { distanceKm(fix.lat, fix.long, it.lat, it.long) <= GPS_LABEL_MAX_KM }
                    ?.displayNameFor(language)
                selectLocation(
                    CalculationLocationDetail(
                        lat = fix.lat,
                        long = fix.long,
                        country = country,
                        label = label,
                    ),
                )
            } finally {
                _uiState.update { it.copy(resolvingGps = false) }
            }
        }
    }

    /** Confirm from the existing "country / city / coordinates" dialog. */
    fun onDialogConfirm(state: NewLocationDialogUiState) {
        val lat = state.latitude.toDoubleOrNull() ?: return
        val long = state.longitude.toDoubleOrNull() ?: return
        viewModelScope.launch {
            selectLocation(
                CalculationLocationDetail(
                    lat = lat,
                    long = long,
                    city = state.selectedCity,
                    country = state.selectedCountry,
                    label = state.label,
                ),
            )
        }
    }

    private suspend fun selectLocation(detail: CalculationLocationDetail) {
        val id = System.currentTimeMillis().toString() + "${detail.lat}"
        val replaced = addedLocationIds.toSet()
        favoriteLocationsRepository.update { current ->
            current.filterNot { it.id in replaced } + StaticFavoriteLocation(id = id, locationDetail = detail)
        }
        addedLocationIds.clear()
        addedLocationIds += id

        val countryCode = detail.country?.code
            ?: detail.city?.country
            ?: withContext(Dispatchers.Default) {
                geoInfoRepository.getCities().nearestTo(detail.lat, detail.long, GPS_COUNTRY_MAX_KM)?.country
            }
        val suggested = suggestedCalculationMethod(countryCode).canonicalParameters()

        calculationSettingsRepository.update { calc ->
            calc.copy(locationId = id, parameters = suggested)
        }
    }

    private suspend fun suggestMethodFor(detail: CalculationLocationDetail) {
        if (suggestionInFlight) return
        suggestionInFlight = true
        try {
            val countryCode = detail.country?.code
                ?: detail.city?.country
                ?: withContext(Dispatchers.Default) {
                    geoInfoRepository.getCities().nearestTo(detail.lat, detail.long, GPS_COUNTRY_MAX_KM)?.country
                }
            val suggested = suggestedCalculationMethod(countryCode).canonicalParameters()
            calculationSettingsRepository.update { calc ->
                if (calc.parameters == null) {
                    calc.copy(parameters = suggested)
                } else {
                    calc
                }
            }
        } finally {
            suggestionInFlight = false
        }
    }
}
