package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ProjectStoreSecurityTest {
    @Test fun editableProjectRejectsAssetPathTraversal() {
        val context=org.robolectric.RuntimeEnvironment.getApplication() as Context
        val store=ProjectStore(context)
        val projectRoot=File(context.filesDir,"studio-projects")
        val folder=File(projectRoot,"test-path-traversal").apply{mkdirs()}
        File(folder,"project.json").writeText("""{"schemaVersion":4,"name":"Unsafe","canvasWidth":1,"canvasHeight":1,"layers":[{"id":"bad","asset":"../../outside.png"}]}""")
        assertNull(store.loadEditableProject(folder))
        folder.deleteRecursively()
    }

    @Test fun snapshotLoadRejectsTraversalAssetPath() {
        val context=org.robolectric.RuntimeEnvironment.getApplication() as Context
        val store=ProjectStore(context)
        val folder=File(context.filesDir,"studio-projects/test-snapshot-path").apply{mkdirs()}
        File(folder,"project.json").writeText("""{"schemaVersion":1,"snapshotAsset":"../../outside.png"}""")
        assertNull(store.loadSnapshot(folder))
        folder.deleteRecursively()
    }
}
