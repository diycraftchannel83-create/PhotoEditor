package com.winsmediatama.winsphotostudio.core.tools

import org.junit.Assert.*
import org.junit.Test

class CropTransformTest {
    @Test fun fullRectIsValidAndHasUnitDimensions() {
        assertTrue(CropRect.FULL.isValid())
        assertEquals(1f, CropRect.FULL.width, 0f)
        assertEquals(1f, CropRect.FULL.height, 0f)
    }

    @Test fun normalizedCropRectReportsDimensions() {
        val rect = CropRect(.1f, .2f, .8f, .9f)
        assertTrue(rect.isValid())
        assertEquals(.7f, rect.width, .0001f)
        assertEquals(.7f, rect.height, .0001f)
    }

    @Test fun rejectsOutOfBoundsAndEmptyRectangles() {
        assertFalse(CropRect(-.1f, 0f, .9f, 1f).isValid())
        assertFalse(CropRect(0f, 0f, 1.1f, 1f).isValid())
        assertFalse(CropRect(.5f, .2f, .5f, .8f).isValid())
        assertFalse(CropRect(.2f, .7f, .8f, .6f).isValid())
    }

    @Test fun aspectPresetsHaveExpectedRatios() {
        assertEquals(1f, CropAspectRatio.SQUARE.width.toFloat()/CropAspectRatio.SQUARE.height, 0f)
        assertEquals(4f/5f, CropAspectRatio.PORTRAIT_4_5.width.toFloat()/CropAspectRatio.PORTRAIT_4_5.height, .0001f)
        assertEquals(16f/9f, CropAspectRatio.WIDE_16_9.width.toFloat()/CropAspectRatio.WIDE_16_9.height, .0001f)
        assertEquals(9f/16f, CropAspectRatio.STORY_9_16.width.toFloat()/CropAspectRatio.STORY_9_16.height, .0001f)
    }
}
