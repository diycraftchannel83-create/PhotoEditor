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
    @Test fun retouchOperationSerializesReplaysAndCanBeDisabled() {
        val src=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val stroke=com.winsmediatama.winsphotostudio.core.tools.RetouchStroke(
            points=listOf(com.winsmediatama.winsphotostudio.core.tools.RetouchPoint(.5f,.5f)),
            size=8f,mode=com.winsmediatama.winsphotostudio.core.tools.RetouchMode.PIXELATE)
        val operation=EditOperation.Retouch(strokes=listOf(stroke),strength=.8f,offsetX=2f,offsetY=-1f)
        val stack=NonDestructiveEditStack(src).apply{append(operation)}
        val restored=NonDestructiveEditStack.fromJson(src,stack.toJson())
        val replayed=restored.render()
        assertEquals(1,restored.operations.size)
        val loaded=restored.operations.first() as EditOperation.Retouch
        assertEquals(stroke.id,loaded.strokes.first().id)
        assertEquals(.8f,loaded.strength,.0001f)
        assertEquals(2f,loaded.offsetX,.0001f)
        assertEquals(-1f,loaded.offsetY,.0001f)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
        assertTrue(restored.toggle(loaded.id,false))
        val disabledReplay=restored.render()
        assertEquals(Color.RED,disabledReplay.getPixel(0,0))
        assertEquals(Color.RED,src.getPixel(0,0))
        replayed.recycle();disabledReplay.recycle();src.recycle()
    }
    @Test fun perspectiveOperationReplaysAndRoundTripsJson() {
        val source=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        val quad=com.winsmediatama.winsphotostudio.core.tools.PerspectiveQuad(
            .05f to .05f,.95f to .05f,.9f to .95f,.1f to .95f)
        val stack=NonDestructiveEditStack(source).apply{append(EditOperation.Perspective(quad=quad,outputWidth=8,outputHeight=8))}
        val rendered=stack.render()
        assertEquals(8,rendered.width);assertEquals(8,rendered.height)
        val restored=NonDestructiveEditStack.fromJson(source,stack.toJson())
        assertTrue(restored.operations.first() is EditOperation.Perspective)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
        assertTrue(restored.toggle(restored.operations.first().id,false))
        assertEquals(Color.BLUE,restored.render().getPixel(0,0))
        rendered.recycle();source.recycle()
    }
    @Test fun canvasSizeOperationReplaysAndRoundTrips() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val stack=NonDestructiveEditStack(source).apply{
            append(EditOperation.CanvasSize(width=4,height=4,anchor=com.winsmediatama.winsphotostudio.core.tools.CanvasAnchor.TOP_LEFT,backgroundColor=Color.TRANSPARENT))
        }
        val output=stack.render()
        assertEquals(4,output.width);assertEquals(4,output.height);assertEquals(Color.RED,output.getPixel(0,0));assertEquals(Color.TRANSPARENT,output.getPixel(3,3))
        val restored=NonDestructiveEditStack.fromJson(source,stack.toJson())
        assertTrue(restored.operations.first() is EditOperation.CanvasSize)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
        assertTrue(restored.toggle(restored.operations.first().id,false))
        assertEquals(2,restored.render().width)
        output.recycle();source.recycle()
    }
    @Test fun backgroundReplacementOperationSerializesAndReplays() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.TRANSPARENT);setPixel(1,0,Color.TRANSPARENT);setPixel(0,1,Color.RED);setPixel(1,1,Color.RED)}
        val stack=NonDestructiveEditStack(source).apply{append(EditOperation.BackgroundReplace(mode="solid",startColor=Color.BLUE))}
        val rendered=stack.render()
        assertEquals(Color.BLUE,rendered.getPixel(0,0))
        assertEquals(Color.RED,rendered.getPixel(0,1))
        val restored=NonDestructiveEditStack.fromJson(source,stack.toJson())
        assertTrue(restored.operations.first() is EditOperation.BackgroundReplace)
        assertEquals(stack.toJson().toString(),restored.toJson().toString())
        assertTrue(restored.toggle(restored.operations.first().id,false))
        assertEquals(Color.TRANSPARENT,restored.render().getPixel(0,0))
        rendered.recycle();source.recycle()
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
