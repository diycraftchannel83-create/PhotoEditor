package com.winsmediatama.winsphotostudio.core.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionPreviewAssemblerTest {
    @Test fun outputSizeKeepsSmallSourceNativeSample() {
        val result = RegionPreviewAssembler.calculateOutputSize(1200, 800, 2048)
        assertEquals(1200, result.width)
        assertEquals(800, result.height)
        assertEquals(1, result.sampleSize)
    }

    @Test fun outputSizeUsesPowerOfTwoSamplingAndBoundsDimension() {
        val result = RegionPreviewAssembler.calculateOutputSize(12000, 8000, 2048)
        assertEquals(4, result.sampleSize)
        assertTrue(result.width <= 2048)
        assertTrue(result.height <= 2048)
    }

    @Test fun outputSizeRespectsCustomPreviewDimension() {
        val result = RegionPreviewAssembler.calculateOutputSize(8192, 5000, 1024)
        assertEquals(8, result.sampleSize)
        assertTrue(result.width <= 1024)
        assertTrue(result.height <= 1024)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroSourceWidth() {
        RegionPreviewAssembler.calculateOutputSize(0, 100, 512)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPreviewDimensionAboveSafetyLimit() {
        RegionPreviewAssembler.calculateOutputSize(1000, 1000, 4096)
    }
}
