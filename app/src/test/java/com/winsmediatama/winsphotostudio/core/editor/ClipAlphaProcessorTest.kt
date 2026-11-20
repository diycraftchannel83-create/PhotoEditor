package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ClipAlphaProcessorTest {
    @Test fun clippingMultipliesSourceAlphaByBelowAlpha() {
        val source=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.argb(255,255,0,0));setPixel(1,0,Color.argb(128,255,0,0))}
        val below=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.argb(128,0,0,255));setPixel(1,0,Color.TRANSPARENT)}
        val result=ClipAlphaProcessor.apply(source,below)
        assertEquals(128,Color.alpha(result.getPixel(0,0)))
        assertEquals(0,Color.alpha(result.getPixel(1,0)))
        assertEquals(255,Color.red(result.getPixel(0,0)))
    }
    @Test fun clippingDoesNotRecycleOrMutateCallerBitmaps() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.RED)}
        val below=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.WHITE)}
        val result=ClipAlphaProcessor.apply(source,below)
        assertEquals(Color.RED,result.getPixel(0,0))
        assertEquals(Color.RED,source.getPixel(0,0))
        assertEquals(Color.WHITE,below.getPixel(0,0))
        assertFalse(source.isRecycled)
        assertFalse(below.isRecycled)
    }
}
