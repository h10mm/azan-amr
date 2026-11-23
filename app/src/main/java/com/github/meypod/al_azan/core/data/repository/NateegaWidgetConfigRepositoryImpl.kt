package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.domain.repository.NateegaWidgetConfigRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow

class NateegaWidgetConfigRepositoryImpl(
    private val nateegaWidgetConfigDatastore: MMKVDataStore<NateegaWidgetConfig>,
) : NateegaWidgetConfigRepository {
    override val data: Flow<NateegaWidgetConfig>
        get() = nateegaWidgetConfigDatastore.data

    override suspend fun fetch(): NateegaWidgetConfig = nateegaWidgetConfigDatastore.data.value

    override suspend fun update(transform: (NateegaWidgetConfig) -> NateegaWidgetConfig) {
        nateegaWidgetConfigDatastore.update(transform)
    }
}
