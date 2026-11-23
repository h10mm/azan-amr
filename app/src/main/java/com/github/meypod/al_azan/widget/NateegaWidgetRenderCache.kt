package com.github.meypod.al_azan.widget

import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData

object NateegaWidgetRenderCache {
    @Volatile
    var lastData: NateegaWidgetData? = null
}
