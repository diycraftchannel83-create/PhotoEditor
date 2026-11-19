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
import com.winsmediatama.winsphotostudio.core.adjustments.HslAdjustments
import com.winsmediatama.winsphotostudio.core.adjustments.HslColorRange
import com.winsmediatama.winsphotostudio.core.adjustments.HslRangeAdjustment
import com.winsmediatama.winsphotostudio.core.adjustments.PhotoEffectKind
import com.winsmediatama.winsphotostudio.core.adjustments.PhotoEffectSettings

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
            append(EditOperation.Effect(settings=PhotoEffectSettings(kind=PhotoEffectKind.VIGNETTE,strength=.3f)))
        }
        val restored=NonDestructiveEditStack.fromJson(src,stack.toJson())
        assertEquals(3,restored.operations.size)
        assertTrue(restored.operations[0] is EditOperation.BasicAdjust)
        assertTrue(restored.operations[1] is EditOperation.Curves)
        assertTrue(restored.operations[2] is EditOperation.Effect)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
    }
    @Test fun lutOperationParsesReplaysAndRoundTrips() {
        val cube="""TITLE "Identity"
LUT_3D_SIZE 2
DOMAIN_MIN 0 0 0
DOMAIN_MAX 1 1 1
0 0 0
0 0 1
0 1 0
0 1 1
1 0 0
1 0 1
1 1 0
1 1 1"""
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.RED)}
        val stack=NonDestructiveEditStack(source).apply{append(EditOperation.Lut(cubeText=cube))}
        assertEquals(Color.RED,stack.render().getPixel(0,0))
        val restored=NonDestructiveEditStack.fromJson(source,stack.toJson())
        assertTrue(restored.operations.first() is EditOperation.Lut)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
    }
    @Test fun transformOperationReplaysAndSerializes() {
        val source=Bitmap.createBitmap(3,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.GREEN)}
        val stack=NonDestructiveEditStack(source).apply{append(EditOperation.Transform(rotationDegrees=90f))}
        val rendered=stack.render();assertEquals(2,rendered.width);assertEquals(3,rendered.height)
        val restored=NonDestructiveEditStack.fromJson(source,stack.toJson())
        assertTrue(restored.operations.first() is EditOperation.Transform)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
    }
    @Test fun hslOperationReplaysAndRoundTripsJson() {
        val src=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.RED)}
        val values=HslAdjustments(mapOf(HslColorRange.RED to HslRangeAdjustment(saturation=-1f)))
        val stack=NonDestructiveEditStack(src).apply{append(EditOperation.Hsl(values=values))}
        val output=stack.render();assertNotEquals(Color.RED,output.getPixel(0,0))
        val restored=NonDestructiveEditStack.fromJson(src,stack.toJson())
        assertTrue(restored.operations.first() is EditOperation.Hsl)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
    }
    @Test fun stackSupportsInsertAndReorder() {
        val src=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
        val first=EditOperation.BasicAdjust(values=BasicAdjustmentValues(brightness=.1f))
        val second=EditOperation.Levels(values=com.winsmediatama.winsphotostudio.core.adjustments.LevelsValues())
        val stack=NonDestructiveEditStack(src).apply{append(first);insert(0,second)}
        assertEquals(second.id,stack.operations.first().id)
        assertTrue(stack.move(first.id,0))
        assertEquals(first.id,stack.operations.first().id)
        assertFalse(stack.move("missing",0))
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
