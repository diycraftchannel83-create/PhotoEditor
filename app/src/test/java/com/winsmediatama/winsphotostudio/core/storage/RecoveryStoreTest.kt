package com.winsmediatama.winsphotostudio.core.storage

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RecoveryStoreTest {
    @Test fun recoverySnapshotRoundTripsLayerMetadataAndPixels() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN)
        val mask=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)}\n        val layer=EditorLayer(name="Recovery test",bitmap=bitmap,opacity=.6f,locked=true,alphaLocked=true,groupId="grp",groupName="Test Group",groupOpacity=.4f,clipToBelow=true,mask=mask)
        store.save(listOf(layer),layer.id,2,2)
        val recovered=store.recover()
        assertNotNull(recovered)
        assertEquals("Recovery test",recovered!!.layers.first().name)
        assertEquals(.6f,recovered.layers.first().opacity,.01f)\n        assertTrue(recovered.layers.first().locked)\n        assertTrue(recovered.layers.first().alphaLocked)\n        assertEquals("grp",recovered.layers.first().groupId)\n        assertEquals("Test Group",recovered.layers.first().groupName)\n        assertEquals(.4f,recovered.layers.first().groupOpacity,.01f)\n        assertTrue(recovered.layers.first().clipToBelow)\n        assertNotNull(recovered.layers.first().mask)
        assertEquals(Color.GREEN,recovered.layers.first().bitmap.getPixel(0,0))
        store.clear()
    }
}
