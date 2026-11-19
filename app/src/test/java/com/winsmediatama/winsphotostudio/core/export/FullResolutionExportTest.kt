package com.winsmediatama.winsphotostudio.core.export

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class FullResolutionExportTest {
    @Test fun originalDimensionsAreSelectedWhenRequested() {
        val preview=Bitmap.createBitmap(100,80,Bitmap.Config.ARGB_8888)
        val size=FullResolutionExport.targetSize(preview,4000,3200,true)
        assertEquals(4000,size.width);assertEquals(3200,size.height)
        preview.recycle()
    }
    @Test fun currentPreviewDimensionsRemainDefault() {
        val preview=Bitmap.createBitmap(100,80,Bitmap.Config.ARGB_8888)
        val size=FullResolutionExport.targetSize(preview,4000,3200,false)
        assertEquals(100,size.width);assertEquals(80,size.height)
        assertSame(preview,FullResolutionExport.render(preview,size))
        preview.recycle()
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsOversizedExport() {
        val preview=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        FullResolutionExport.targetSize(preview,10000,10000,true)
        preview.recycle()
    }
}
