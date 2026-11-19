package com.winsmediatama.winsphotostudio.core.storage

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProjectStoreLifecycleTest {
    @Test
    fun snapshotProjectCanBeLoadedRenamedDuplicatedAndDeleted() {
        val context = RuntimeEnvironment.getApplication()
        val store = ProjectStore(context)
        val bitmap = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }

        val project = store.saveSnapshot(bitmap, "Lifecycle Test")
        assertTrue(project.isDirectory)
        assertEquals("Lifecycle Test", store.projectName(project))

        val restored = store.loadSnapshot(project)
        assertNotNull(restored)
        assertEquals(12, restored!!.width)
        assertEquals(8, restored.height)
        assertEquals(Color.MAGENTA, restored.getPixel(0, 0))

        assertTrue(store.renameProject(project, "Renamed Project"))
        assertEquals("Renamed Project", store.projectName(project))

        val duplicate = store.duplicateProject(project, "Copy Project")
        assertNotNull(duplicate)
        assertEquals("Copy Project", store.projectName(duplicate!!))
        assertTrue(store.listProjects().any { it.name == project.name })
        assertTrue(store.deleteProject(duplicate.name))
        assertFalse(duplicate.exists())
        assertTrue(project.exists())
        restored.recycle()
        bitmap.recycle()
    }

    @Test
    fun blankRenameIsRejectedWithoutChangingProjectName() {
        val context = RuntimeEnvironment.getApplication()
        val store = ProjectStore(context)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val project = store.saveSnapshot(bitmap, "Keep Name")
        assertFalse(store.renameProject(project, "   "))
        assertEquals("Keep Name", store.projectName(project))
        bitmap.recycle()
    }
}
