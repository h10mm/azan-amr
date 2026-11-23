package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import kotlinx.coroutines.flow.Flow

interface NateegaWidgetConfigRepository {
    val data: Flow<NateegaWidgetConfig>
    suspend fun fetch(): NateegaWidgetConfig
    suspend fun update(transform: (NateegaWidgetConfig) -> NateegaWidgetConfig)
}
