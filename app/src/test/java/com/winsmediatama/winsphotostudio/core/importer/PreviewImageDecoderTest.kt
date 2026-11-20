package com.winsmediatama.winsphotostudio.core.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewImageDecoderTest {
    @Test fun sampleSizeKeepsSmallImagesUnscaled() {
        assertEquals(1, PreviewImageDecoder.calculateSampleSize(1200, 800))
    }

    @Test fun sampleSizeCapsLargestDimensionAtPreviewLimit() {
        assertEquals(2, PreviewImageDecoder.calculateSampleSize(4096, 2160))
        assertEquals(8, PreviewImageDecoder.calculateSampleSize(12000, 8000))
    }

    @Test fun sampleSizeHandlesInvalidBoundsWithoutThrowing() {
        assertEquals(1, PreviewImageDecoder.calculateSampleSize(0, -10))
    }

    @Test fun customMaximumDimensionIsRespected() {
        assertEquals(8, PreviewImageDecoder.calculateSampleSize(8192, 5000, 1024))
    }
}
