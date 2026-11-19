package com.winsmediatama.winsphotostudio.core.tools

import org.junit.Assert.*
import org.junit.Test

class RetouchStrokeModelTest {
    @Test fun strokeRoundTripsThroughJsonCodec() {
        val original = listOf(
            RetouchStroke(points=listOf(RetouchPoint(.1f,.2f),RetouchPoint(.7f,.8f)),size=42f,mode=RetouchMode.CLONE),
            RetouchStroke(points=listOf(RetouchPoint(.5f,.5f)),size=16f,mode=RetouchMode.SPOT_HEAL)
        )
        val decoded = RetouchStrokeCodec.decode(RetouchStrokeCodec.encode(original))
        assertEquals(2, decoded.size)
        assertEquals(original[0].id, decoded[0].id)
        assertEquals(original[0].mode, decoded[0].mode)
        assertEquals(original[0].size, decoded[0].size, 0.001f)
        assertEquals(original[0].points, decoded[0].points)
    }
    @Test fun invalidPointsAndSizesAreRejected() {
        assertFalse(RetouchStroke(points=listOf(RetouchPoint(1.2f,0f)),size=10f,mode=RetouchMode.BLUR).valid())
        assertFalse(RetouchStroke(points=listOf(RetouchPoint(.2f,.3f)),size=0f,mode=RetouchMode.BLUR).valid())
        assertTrue(RetouchStroke(points=listOf(RetouchPoint(.2f,.3f)),size=10f,mode=RetouchMode.BLUR).valid())
    }
    @Test fun decoderSkipsInvalidEntriesAndMalformedJsonSafely() {
        val valid = RetouchStroke(points=listOf(RetouchPoint(.3f,.4f)),size=12f,mode=RetouchMode.BURN).toJson()
        val invalid = RetouchStroke(points=listOf(RetouchPoint(2f,0f)),size=12f,mode=RetouchMode.BURN).toJson()
        val decoded = RetouchStrokeCodec.decode(org.json.JSONArray().put(valid).put(invalid).toString())
        assertEquals(1, decoded.size)
        assertTrue(RetouchStrokeCodec.decode("not-json").isEmpty())
    }
}