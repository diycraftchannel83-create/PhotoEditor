package com.winsmediatama.winsphotostudio.core.storage

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import android.content.Context
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
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=RecoveryStore(context)
        val bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN)
        val layer=EditorLayer(name="Recovery test",bitmap=bitmap,opacity=.6f)
        store.save(listOf(layer),layer.id,2,2)
        val recovered=store.recover()
        assertNotNull(recovered)
        assertEquals("Recovery test",recovered!!.layers.first().name)
        assertEquals(.6f,recovered.layers.first().opacity,.01f)
        assertEquals(Color.GREEN,recovered.layers.first().bitmap.getPixel(0,0))
        store.clear()
    }
}
