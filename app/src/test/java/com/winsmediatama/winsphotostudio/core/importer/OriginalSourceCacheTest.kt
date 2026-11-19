package com.winsmediatama.winsphotostudio.core.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalSourceCacheTest {
    @Test fun smallSourceUsesFullDecode() {
        assertEquals(1, OriginalSourceCache.calculateSampleSizeForPixelLimit(1600,1200,24_000_000L))
    }

    @Test fun largeSourceSamplesDownToPixelBudget() {
        val sample=OriginalSourceCache.calculateSampleSizeForPixelLimit(12000,8000,24_000_000L)
        assertTrue(sample>1)
        assertTrue((12000L/sample)*(8000L/sample)<=24_000_000L)
    }

    @Test fun invalidDimensionsRejected() {
        runCatching { OriginalSourceCache.calculateSampleSizeForPixelLimit(0,100,1000) }
            .onSuccess { throw AssertionError("Expected invalid bounds rejection") }
    }

    @Test fun invalidPixelBudgetRejected() {
        runCatching { OriginalSourceCache.calculateSampleSizeForPixelLimit(100,100,24_000_001L) }
            .onSuccess { throw AssertionError("Expected pixel budget rejection") }
    }
}
