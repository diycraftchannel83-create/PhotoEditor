package com.winsmediatama.winsphotostudio.core.export

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSourceLayerExportTest {
    @Test
    fun sourceLayerIsCompositedAtNativeSizeWhenRetainedSourceExists() {
        val preview=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val source=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.GREEN)}
        val layer=EditorLayer(name="Background",bitmap=preview,originalBitmap=preview)
        val output=NativeSourceLayerExport.render(source,listOf(layer),layer.id,2,2)
        assertNotNull(output)
        assertEquals(4,output!!.width)
        assertEquals(4,output.height)
        assertEquals(Color.GREEN,output.getPixel(0,0))
        output.recycle();source.recycle();preview.recycle()
    }

    @Test
    fun sourceLayerWithoutRetainedReplaySourceFallsBack() {
        val preview=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val source=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.GREEN)}
        val layer=EditorLayer(name="Edited",bitmap=preview)
        assertNull(NativeSourceLayerExport.render(source,listOf(layer),layer.id,2,2))
        source.recycle();preview.recycle()
    }
}
