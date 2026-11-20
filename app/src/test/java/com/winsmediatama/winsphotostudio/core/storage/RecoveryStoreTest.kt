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
    @Test fun replacementSnapshotKeepsLatestStateAndClearsOldAssets() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val first=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val second=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        store.save(listOf(EditorLayer(name="first",bitmap=first)),null,1,1)
        store.save(listOf(EditorLayer(name="second",bitmap=second)),null,1,1)
        val recovered=store.recover()
        assertNotNull(recovered)
        assertEquals("second",recovered!!.layers.single().name)
        assertEquals(Color.BLUE,recovered.layers.single().bitmap.getPixel(0,0))
        store.clear()
    }

    @Test fun oversizedRecoveryCanvasIsRejectedBeforeWritingSnapshot() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val tiny=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        val failure=runCatching{store.save(listOf(EditorLayer(name="tiny",bitmap=tiny)),null,6000,5000)}.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        store.clear()
    }
    @Test fun corruptRecoveryManifestAndUnsupportedSchemaReturnNull() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val tiny=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        store.save(listOf(EditorLayer(name="safe",bitmap=tiny)),null,1,1)
        val root=java.io.File(context.filesDir,"studio-recovery")
        val manifest=java.io.File(root,"recovery.json")
        manifest.writeText("{bad")
        assertNull(store.recover())
        manifest.writeText(org.json.JSONObject().put("snapshot","snapshot/recovery.json").toString())
        val snapshot=java.io.File(root,"snapshot/recovery.json")
        val json=org.json.JSONObject(snapshot.readText()).put("schemaVersion",99)
        snapshot.writeText(json.toString())
        assertNull(store.recover())
        store.clear()
    }

    @Test fun recoveryRejectsTraversalSnapshotAssetPath() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val tiny=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        store.save(listOf(EditorLayer(name="safe",bitmap=tiny)),null,1,1)
        val root=java.io.File(context.filesDir,"studio-recovery")
        val snapshot=java.io.File(root,"snapshot/recovery.json")
        val json=org.json.JSONObject(snapshot.readText())
        val layers=json.getJSONArray("layers")
        layers.getJSONObject(0).put("asset","../../outside.png")
        snapshot.writeText(json.toString())
        assertNull(store.recover())
        store.clear()
    }
    @Test fun legacyRecoverySchemaOneLoadsMissingOptionalMetadataDefaults() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=RecoveryStore(context)
        val root=java.io.File(context.filesDir,"studio-recovery")
        val snapshotDir=java.io.File(root,"snapshot").apply{mkdirs()}
        val asset=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.YELLOW)}
        java.io.File(snapshotDir,"legacy.png").outputStream().use{asset.compress(Bitmap.CompressFormat.PNG,100,it)}
        val layers=org.json.JSONArray().put(org.json.JSONObject().put("id","legacy").put("name","Legacy").put("asset","legacy.png"))
        val recovery=org.json.JSONObject().put("schemaVersion",1).put("canvasWidth",2).put("canvasHeight",2).put("layers",layers)
        java.io.File(snapshotDir,"recovery.json").writeText(recovery.toString())
        java.io.File(root,"recovery.json").writeText(org.json.JSONObject().put("snapshot","snapshot/recovery.json").toString())
        val restored=store.recover()
        assertNotNull(restored)
        assertEquals("Legacy",restored!!.layers.single().name)
        assertEquals(1f,restored.layers.single().opacity,.001f)
        assertFalse(restored.layers.single().locked)
        assertEquals(Color.YELLOW,restored.layers.single().bitmap.getPixel(0,0))
        store.clear()
    }
}
