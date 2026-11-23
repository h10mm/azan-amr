package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.calculation.withEgyptianFixApplied
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CalculationSettingsRepositoryImpl(
    private val calcSettingsDatastore: MMKVDataStore<CalculationSettings>,
) : CalculationSettingsRepository {
    override val data: Flow<CalculationSettings>
        get() = calcSettingsDatastore.data.map { it.withEgyptianFixApplied() }.distinctUntilChanged()

    override suspend fun fetch(): CalculationSettings =
        calcSettingsDatastore.data.value.withEgyptianFixApplied()

    override suspend fun update(transform: (t: CalculationSettings) -> CalculationSettings) {
        calcSettingsDatastore.update { transform(it).withEgyptianFixApplied() }
    }
}
