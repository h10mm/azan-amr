package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.CalculationMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EgyptianMethodParametersTest {
    @Test
    fun canonicalParameters_clearsDhuhrAdjustmentForEgyptian() {
        val params = CalculationMethod.EGYPTIAN.canonicalParameters()
        assertEquals(0, params.methodAdjustments.dhuhr)
        assertEquals(19.5, params.fajrAngle, 0.0)
        assertEquals(17.5, params.ishaAngle, 0.0)
    }

    @Test
    fun canonicalParameters_preservesOtherMethods() {
        val ummAlQura = CalculationMethod.UMM_AL_QURA.canonicalParameters()
        assertEquals(CalculationMethod.UMM_AL_QURA.parameters, ummAlQura)
    }

    @Test
    fun withEgyptianFixApplied_clearsDhuhrOffset() {
        val settings = CalculationSettings(parameters = CalculationMethod.EGYPTIAN.parameters)
        val fixed = settings.withEgyptianFixApplied()
        assertEquals(0, fixed.parameters?.methodAdjustments?.dhuhr)
    }

    @Test
    fun withEgyptianFixApplied_isIdempotent() {
        val fixedOnce = CalculationSettings(parameters = CalculationMethod.EGYPTIAN.parameters).withEgyptianFixApplied()
        val fixedTwice = fixedOnce.withEgyptianFixApplied()
        assertSame(fixedOnce, fixedTwice)
    }
}
