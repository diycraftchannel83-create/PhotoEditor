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
class ProjectStoreTest {
    @Test fun editableProjectRoundTripsLayerMetadataAndPixels() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=ProjectStore(context)
        val bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.CYAN)}
        val mask=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)}
        val original=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val operations=listOf(EditOperation.Hsl(values=HslAdjustments(mapOf(HslColorRange.RED to HslRangeAdjustment(saturation=-.5f)))),EditOperation.Transform(rotationDegrees=90f))
        val layer=EditorLayer(name="Layer test",bitmap=bitmap,opacity=.7f,locked=true,alphaLocked=true,groupId="g",groupName="Group",groupOpacity=.5f,groupBlendMode=EditorBlendMode.MULTIPLY,clipToBelow=true,mask=mask,originalBitmap=original,operations=operations)
        val folder=store.saveLayerProject(listOf(layer),layer.id,2,2,"Round trip")
        val loaded=store.loadEditableProject(folder)
        assertNotNull(loaded)
        assertEquals("Round trip",loaded!!.name)
        val actual=loaded.layers.first()
        assertEquals("Layer test",actual.name);assertEquals(.7f,actual.opacity,.01f)
        assertTrue(actual.locked);assertTrue(actual.alphaLocked);assertTrue(actual.clipToBelow)
        assertEquals("g",actual.groupId);assertEquals("Group",actual.groupName);assertEquals(.5f,actual.groupOpacity,.01f);assertEquals(EditorBlendMode.MULTIPLY,actual.groupBlendMode)
        assertNotNull(actual.mask);assertEquals(Color.CYAN,actual.bitmap.getPixel(0,0))
        assertNotNull(actual.originalBitmap);assertEquals(Color.RED,actual.originalBitmap!!.getPixel(0,0))
        assertEquals(2,actual.operations.size);assertTrue(actual.operations.first() is EditOperation.Hsl);assertTrue(actual.operations.last() is EditOperation.Transform)
        assertTrue(store.renameProject(folder,"Renamed"))
        assertEquals("Renamed",store.projectName(folder))
        val copy=store.duplicateProject(folder)
        assertNotNull(copy);assertEquals("Renamed Copy",store.projectName(copy!!))
        assertTrue(store.deleteProject(copy.name));assertTrue(store.deleteProject(folder.name))
    }
    @Test fun corruptedManifestAndUnsupportedSchemaFailSafely() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=ProjectStore(context)
        val bitmap=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        val folder=store.saveLayerProject(listOf(EditorLayer(name="safe",bitmap=bitmap)),null,1,1,"Corruption")
        val manifest=java.io.File(folder,"project.json")
        manifest.writeText("{broken json")
        assertNull(store.loadEditableProject(folder))
        manifest.writeText(org.json.JSONObject().put("schemaVersion",99).put("name","future").toString())
        assertNull(store.loadEditableProject(folder))
        store.deleteProject(folder.name)
    }

    @Test fun editableLoaderRejectsTraversalAssetPath() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=ProjectStore(context)
        val bitmap=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        val folder=store.saveLayerProject(listOf(EditorLayer(name="safe",bitmap=bitmap)),null,1,1,"Traversal")
        val manifest=org.json.JSONObject(java.io.File(folder,"project.json").readText())
        val layers=manifest.getJSONArray("layers")
        layers.getJSONObject(0).put("asset","../outside.png")
        java.io.File(folder,"project.json").writeText(manifest.toString())
        assertNull(store.loadEditableProject(folder))
        store.deleteProject(folder.name)
    }
    @Test fun projectLifecycleRejectsTraversalIdentifiersAndFolders() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=ProjectStore(context)
        assertFalse(store.deleteProject("../outside"))
        val outside=java.io.File(context.filesDir,"not-a-project").apply{mkdirs()}
        assertFalse(store.renameProject(outside,"bad rename"))
        assertNull(store.duplicateProject(outside))
        outside.deleteRecursively()
    }
    @Test fun projectSaveRejectsOversizedCanvasAndLayerCount() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        val store=ProjectStore(context)
        val tiny=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        assertTrue(runCatching{store.saveLayerProject(listOf(EditorLayer(name="tiny",bitmap=tiny)),null,6000,5000,"too big")}.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching{store.saveLayerProject(emptyList(),null,1,1,"empty")}.exceptionOrNull() is IllegalArgumentException)
    }
}
