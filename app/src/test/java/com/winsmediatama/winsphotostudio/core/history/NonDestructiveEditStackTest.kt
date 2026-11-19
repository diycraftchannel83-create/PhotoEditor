package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import com.winsmediatama.winsphotostudio.core.adjustments.CurvePoint
import com.winsmediatama.winsphotostudio.core.adjustments.CurvesValues

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NonDestructiveEditStackTest {
    @Test fun originalBitmapIsNotModifiedByReplay() {
        val src=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.RED)
        val stack=NonDestructiveEditStack(src).apply { append(EditOperation.BasicAdjust(values=BasicAdjustmentValues(brightness=.3f))) }
        val rendered=stack.render()
        assertEquals(Color.RED,src.getPixel(0,0))
        assertNotEquals(src.getPixel(0,0),rendered.getPixel(0,0))
    }
    @Test fun disabledOperationIsSkipped() {
        val src=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);src.setPixel(0,0,Color.RED)
        val stack=NonDestructiveEditStack(src).apply { append(EditOperation.BasicAdjust(enabled=false,values=BasicAdjustmentValues(brightness=.5f))) }
        assertEquals(Color.RED,stack.render().getPixel(0,0))
    }
    @Test fun operationStackRoundTripsJson() {
        val src=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);src.setPixel(0,0,Color.GRAY)
        val stack=NonDestructiveEditStack(src).apply {
            append(EditOperation.BasicAdjust(values=BasicAdjustmentValues(exposure=1f)))
            append(EditOperation.Curves(values=CurvesValues(composite=listOf(CurvePoint(0f,0f),CurvePoint(.5f,.7f),CurvePoint(1f,1f)))))
        }
        val restored=NonDestructiveEditStack.fromJson(src,stack.toJson())
        assertEquals(2,restored.operations.size)
        assertTrue(restored.operations[0] is EditOperation.BasicAdjust)
        assertTrue(restored.operations[1] is EditOperation.Curves)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
    }
    @Test fun cropOperationChangesOutputSizeAndKeepsOriginal() {
        val src=Bitmap.createBitmap(10,8,Bitmap.Config.ARGB_8888);src.eraseColor(Color.BLUE)
        val stack=NonDestructiveEditStack(src).apply { append(EditOperation.Crop(left=.2f,top=.25f,right=.8f,bottom=.75f)) }
        val output=stack.render()
        assertEquals(6,output.width);assertEquals(4,output.height)
        assertEquals(10,src.width);assertEquals(8,src.height)
    }
    @Test fun cropRejectsInvalidBounds() {
        val src=Bitmap.createBitmap(3,3,Bitmap.Config.ARGB_8888)
        val stack=NonDestructiveEditStack(src).apply { append(EditOperation.Crop(left=.8f,top=0f,right=.2f,bottom=1f)) }
        assertThrows(IllegalArgumentException::class.java){ stack.render() }
    }
}
