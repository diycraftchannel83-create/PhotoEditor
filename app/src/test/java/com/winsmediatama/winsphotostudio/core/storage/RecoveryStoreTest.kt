package com.winsmediatama.winsphotostudio.core.storage

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.adjustments.HslAdjustments
import com.winsmediatama.winsphotostudio.core.adjustments.HslColorRange
import com.winsmediatama.winsphotostudio.core.adjustments.HslRangeAdjustment

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RecoveryStoreTest {
    @Test fun recoverySnapshotRoundTripsLayerMetadataAndPixels() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN)
        val mask=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)}
        val original=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val operations=listOf(EditOperation.Hsl(values=HslAdjustments(mapOf(HslColorRange.RED to HslRangeAdjustment(saturation=-.5f)))))
        val layer=EditorLayer(name="Recovery test",bitmap=bitmap,opacity=.6f,locked=true,alphaLocked=true,groupId="grp",groupName="Test Group",groupOpacity=.4f,groupBlendMode=EditorBlendMode.SCREEN,clipToBelow=true,mask=mask,originalBitmap=original,operations=operations)
        store.save(listOf(layer),layer.id,2,2)
        val recovered=store.recover()
        assertNotNull(recovered)
        assertEquals("Recovery test",recovered!!.layers.first().name)
        assertEquals(.6f,recovered.layers.first().opacity,.01f)
        assertTrue(recovered.layers.first().locked)
        assertTrue(recovered.layers.first().alphaLocked)
        assertEquals("grp",recovered.layers.first().groupId)
        assertEquals("Test Group",recovered.layers.first().groupName)
        assertEquals(.4f,recovered.layers.first().groupOpacity,.01f)
        assertTrue(recovered.layers.first().clipToBelow)
        assertNotNull(recovered.layers.first().mask)
        assertNotNull(recovered.layers.first().originalBitmap)
        assertEquals(1,recovered.layers.first().operations.size)
        assertTrue(recovered.layers.first().operations.first() is EditOperation.Hsl)
        assertEquals(Color.GREEN,recovered.layers.first().bitmap.getPixel(0,0))
        store.clear()
    }
}
