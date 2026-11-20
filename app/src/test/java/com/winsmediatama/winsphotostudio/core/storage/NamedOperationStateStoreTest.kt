package com.winsmediatama.winsphotostudio.core.storage

import android.graphics.Bitmap
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NamedOperationStateStoreTest {
    @Test fun namedStatePersistsAcrossStoreInstancesAndCanBeRemoved() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val layerId="layer-test-42"
        val stateStore=NamedOperationStateStore(context)
        val operation=EditOperation.BasicAdjust(values=BasicAdjustmentValues(brightness=.35f,contrast=.2f))
        assertTrue(stateStore.save(layerId,"Bright look",listOf(operation)))
        val reopened=NamedOperationStateStore(context)
        assertEquals(listOf("Bright look"),reopened.names(layerId))
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888)
        val restored=reopened.load(layerId,"Bright look",source)
        assertNotNull(restored)
        assertEquals(1,restored!!.size)
        val basic=restored.single() as EditOperation.BasicAdjust
        assertEquals(.35f,basic.values.brightness,.001f)
        assertEquals(.2f,basic.values.contrast,.001f)
        assertTrue(reopened.remove(layerId,"Bright look"))
        assertTrue(reopened.names(layerId).isEmpty())
    }

    @Test fun blankOrOversizedNameIsRejectedAndStateCountIsBounded() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=NamedOperationStateStore(context)
        val layerId="bounded-test"
        assertFalse(store.save(layerId,"   ",emptyList()))
        assertFalse(store.save(layerId,"x".repeat(81),emptyList()))
        repeat(50){assertTrue(store.save(layerId,"State $it",emptyList()))}
        assertFalse(store.save(layerId,"State 51",emptyList()))
        assertEquals(50,store.names(layerId).size)
        store.remove(layerId,"State 0")
    }
}
