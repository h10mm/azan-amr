package com.github.meypod.al_azan.main.tasbih

/**
 * Lightweight volume-key interceptor bus used while [TasbihScreen] is active on screen
 * so hardware Volume Up / Volume Down presses increment the tasbih count cleanly.
 */
object TasbihVolumeKeyBus {
    @Volatile
    var onVolumeKeyIncrement: (() -> Unit)? = null
}
