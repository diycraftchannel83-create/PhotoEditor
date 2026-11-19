package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

private enum class TextAlignment { LEFT, CENTER, RIGHT }
private enum class OverlayKind(val title:String) { TEXT("Text"), RECTANGLE("Rectangle"), ELLIPSE("Ellipse"), LINE("Line") }

@Composable
fun TextShapeScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var kind by remember{mutableStateOf(OverlayKind.TEXT)}
    var text by remember{mutableStateOf("WinsPhoto Studio")}
    val context=LocalContext.current
    var textImportMessage by remember{mutableStateOf<String?>(null)}
    val textFilePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}}
            .onSuccess{loaded->if(loaded!=null){text=loaded;textImportMessage="Text imported"}else textImportMessage="Could not read text file"}
            .onFailure{textImportMessage=it.message}
    }
    var overlayColor by remember{mutableStateOf(Color.White)}
    var outlineColor by remember{mutableStateOf(Color.Black)}
    var fontSize by remember{mutableFloatStateOf(72f)}
    var x by remember{mutableFloatStateOf(.5f)}
    var y by remember{mutableFloatStateOf(.5f)}
    var opacity by remember{mutableFloatStateOf(1f)}
    var rotation by remember{mutableFloatStateOf(0f)}
    var bold by remember{mutableStateOf(false)}
    var italic by remember{mutableStateOf(false)}
    var outline by remember{mutableStateOf(false)}
    var outlineWidth by remember{mutableFloatStateOf(4f)}
    var alignment by remember{mutableStateOf(TextAlignment.CENTER)}
    var fontFamily by remember{mutableStateOf("sans-serif")}
    var tracking by remember{mutableFloatStateOf(0f)}
    var lineSpacing by remember{mutableFloatStateOf(0f)}
    var fontMenu by remember{mutableStateOf(false)}
    var shadow by remember{mutableStateOf(false)}
    var shadowRadius by remember{mutableFloatStateOf(8f)}
    var shapeFill by remember{mutableStateOf(false)}
    var shapeWidth by remember{mutableFloatStateOf(.24f)}
    var shapeHeight by remember{mutableFloatStateOf(.16f)}
    val palette=listOf(Color.White,Color.Black,Color(0xFFFF3B30),Color(0xFFFFC107),Color(0xFF00C853),Color(0xFF00B0FF),Color(0xFF7C4DFF),Color(0xFFFF4081))
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text("Text & Shapes",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(renderOverlay(source,kind,text,overlayColor,outlineColor,fontSize,x,y,opacity,rotation,bold,italic,outline,outlineWidth,alignment,fontFamily,shadow,shadowRadius,tracking,lineSpacing,shapeFill,shapeWidth,shapeHeight))}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D))) {
            Image(source.asImageBitmap(),null,Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val nc=drawContext.canvas.nativeCanvas
                val scale=size.width/source.width
                val cx=x*size.width;val cy=y*size.height
                val p=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{
                    color=overlayColor.copy(alpha=opacity).toArgb()
                    textSize=fontSize*scale
                    strokeWidth=(outlineWidth*scale).coerceAtLeast(1f)
                    typeface=Typeface.create(fontFamily,(if(bold)Typeface.BOLD else Typeface.NORMAL)or(if(italic)Typeface.ITALIC else 0))
                    textAlign=when(alignment){TextAlignment.LEFT->AndroidPaint.Align.LEFT;TextAlignment.CENTER->AndroidPaint.Align.CENTER;TextAlignment.RIGHT->AndroidPaint.Align.RIGHT}
                    letterSpacing=tracking/100f
                    if(shadow&&kind==OverlayKind.TEXT)setShadowLayer(shadowRadius*scale,2f*scale,2f*scale,android.graphics.Color.BLACK)
                }
                nc.save();nc.rotate(rotation,cx,cy)
                when(kind){
                    OverlayKind.TEXT->{val lines=text.split('\n');val step=p.textSize+lineSpacing*scale;val startY=cy-(lines.size-1)*step/2f;if(outline){val stroke=AndroidPaint(p).apply{color=outlineColor.toArgb();style=AndroidPaint.Style.STROKE;strokeWidth=outlineWidth*scale;clearShadowLayer()};lines.forEachIndexed{index,line->nc.drawText(line,cx,startY+index*step,stroke)}};p.style=AndroidPaint.Style.FILL;lines.forEachIndexed{index,line->nc.drawText(line,cx,startY+index*step,p)}}
                    OverlayKind.RECTANGLE->{p.style=if(shapeFill)AndroidPaint.Style.FILL else AndroidPaint.Style.STROKE;nc.drawRect(cx-size.width*shapeWidth/2,cy-size.height*shapeHeight/2,cx+size.width*shapeWidth/2,cy+size.height*shapeHeight/2,p)}
                    OverlayKind.ELLIPSE->{p.style=if(shapeFill)AndroidPaint.Style.FILL else AndroidPaint.Style.STROKE;nc.drawOval(RectF(cx-size.width*shapeWidth/2,cy-size.height*shapeHeight/2,cx+size.width*shapeWidth/2,cy+size.height*shapeHeight/2),p)}
                    OverlayKind.LINE->{p.style=AndroidPaint.Style.STROKE;nc.drawLine(cx-size.width*shapeWidth/2,cy,cx+size.width*shapeWidth/2,cy,p)}
                }
                nc.restore()
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){OverlayKind.entries.forEach{k->FilterChip(selected=kind==k,onClick={kind=k},label={Text(k.title)})}}
            if(kind==OverlayKind.TEXT){
                OutlinedTextField(value=text,onValueChange={text=it},label={Text("Text")},modifier=Modifier.fillMaxWidth())
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){TextButton(onClick={textFilePicker.launch(arrayOf("text/plain","text/*"))}){Text("Import text file")};textImportMessage?.let{Text(it,color=Color.LightGray,style=MaterialTheme.typography.labelSmall)}}
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("Font: $fontFamily",Modifier.weight(1f),color=Color.LightGray);TextButton(onClick={fontMenu=true}){Text("Choose")};DropdownMenu(expanded=fontMenu,onDismissRequest={fontMenu=false}){listOf("sans-serif","serif","monospace","sans-serif-condensed","sans-serif-light","sans-serif-medium","cursive","casual").forEach{family->DropdownMenuItem(text={Text(family)},onClick={fontFamily=family;fontMenu=false})}}}
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(checked=bold,onCheckedChange={bold=it});Text("Bold",color=Color.White);Checkbox(checked=italic,onCheckedChange={italic=it});Text("Italic",color=Color.White);Checkbox(checked=outline,onCheckedChange={outline=it});Text("Outline",color=Color.White)}
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){TextAlignment.entries.forEach{a->FilterChip(selected=alignment==a,onClick={alignment=a},label={Text(a.name)})}}
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(checked=shadow,onCheckedChange={shadow=it});Text("Shadow",color=Color.White)}
                if(shadow){Text("Shadow radius ${shadowRadius.toInt()}px",color=Color.LightGray);Slider(value=shadowRadius,onValueChange={shadowRadius=it},valueRange=0f..32f)}
                if(outline){Text("Outline width ${outlineWidth.toInt()}px",color=Color.LightGray);Slider(value=outlineWidth,onValueChange={outlineWidth=it},valueRange=1f..24f);Text("Outline color",color=Color.LightGray);ColorPalette(palette,onSelect={outlineColor=it})}
                Text("Letter spacing ${tracking.toInt()}px",color=Color.LightGray);Slider(value=tracking,onValueChange={tracking=it},valueRange=0f..24f)
                Text("Line spacing ${lineSpacing.toInt()}px",color=Color.LightGray);Slider(value=lineSpacing,onValueChange={lineSpacing=it},valueRange=0f..80f)
            }
            Text("Fill color",color=Color.LightGray);ColorPalette(palette,onSelect={overlayColor=it})
            if(kind!=OverlayKind.TEXT){Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(checked=shapeFill,onCheckedChange={shapeFill=it});Text("Filled shape",color=Color.White)};Text("Width ${(shapeWidth*100).toInt()}%",color=Color.LightGray);Slider(value=shapeWidth,onValueChange={shapeWidth=it},valueRange=.05f..1f);Text("Height ${(shapeHeight*100).toInt()}%",color=Color.LightGray);Slider(value=shapeHeight,onValueChange={shapeHeight=it},valueRange=.05f..1f)}
            Text("Size ${fontSize.toInt()}px",color=Color.LightGray);Slider(value=fontSize,onValueChange={fontSize=it},valueRange=12f..240f)
            Text("Rotation ${rotation.toInt()}°",color=Color.LightGray);Slider(value=rotation,onValueChange={rotation=it},valueRange=-180f..180f)
            Text("X ${(x*100).toInt()}%",color=Color.LightGray);Slider(value=x,onValueChange={x=it},valueRange=0f..1f)
            Text("Y ${(y*100).toInt()}%",color=Color.LightGray);Slider(value=y,onValueChange={y=it},valueRange=0f..1f)
            Text("Opacity ${(opacity*100).toInt()}%",color=Color.LightGray);Slider(value=opacity,onValueChange={opacity=it},valueRange=0f..1f)
        }
    }
}
@Composable private fun ColorPalette(colors:List<Color>,onSelect:(Color)->Unit){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){colors.forEach{c->Button(onClick={onSelect(c)},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(30.dp),contentPadding=PaddingValues(0.dp)){} }}}
private fun renderOverlay(source:Bitmap,kind:OverlayKind,text:String,color:Color,outlineColor:Color,fontSize:Float,x:Float,y:Float,opacity:Float,rotation:Float,bold:Boolean,italic:Boolean,outline:Boolean,outlineWidth:Float,alignment:TextAlignment,fontFamily:String,shadow:Boolean,shadowRadius:Float,tracking:Float,lineSpacing:Float,shapeFill:Boolean,shapeWidth:Float,shapeHeight:Float):Bitmap {
    val output=source.copy(Bitmap.Config.ARGB_8888,true);val canvas=AndroidCanvas(output)
    val p=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{this.color=color.copy(alpha=opacity).toArgb();textSize=fontSize;strokeWidth=outlineWidth.coerceAtLeast(1f);typeface=Typeface.create(fontFamily,(if(bold)Typeface.BOLD else Typeface.NORMAL)or(if(italic)Typeface.ITALIC else 0));textAlign=when(alignment){TextAlignment.LEFT->AndroidPaint.Align.LEFT;TextAlignment.CENTER->AndroidPaint.Align.CENTER;TextAlignment.RIGHT->AndroidPaint.Align.RIGHT};letterSpacing=tracking/100f;if(shadow&&kind==OverlayKind.TEXT)setShadowLayer(shadowRadius,2f,2f,android.graphics.Color.BLACK)}
    val cx=x*source.width;val cy=y*source.height
    canvas.save();canvas.rotate(rotation,cx,cy)
    when(kind){
        OverlayKind.TEXT->{val lines=text.split('\n');val step=p.textSize+lineSpacing;val startY=cy-(lines.size-1)*step/2f;if(outline){val stroke=AndroidPaint(p).apply{this.color=outlineColor.toArgb();style=AndroidPaint.Style.STROKE;strokeWidth=outlineWidth;clearShadowLayer()};lines.forEachIndexed{index,line->canvas.drawText(line,cx,startY+index*step,stroke)}};p.style=AndroidPaint.Style.FILL;lines.forEachIndexed{index,line->canvas.drawText(line,cx,startY+index*step,p)}}
        OverlayKind.RECTANGLE->{p.style=if(shapeFill)AndroidPaint.Style.FILL else AndroidPaint.Style.STROKE;canvas.drawRect(cx-source.width*shapeWidth/2,cy-source.height*shapeHeight/2,cx+source.width*shapeWidth/2,cy+source.height*shapeHeight/2,p)}
        OverlayKind.ELLIPSE->{p.style=if(shapeFill)AndroidPaint.Style.FILL else AndroidPaint.Style.STROKE;canvas.drawOval(RectF(cx-source.width*shapeWidth/2,cy-source.height*shapeHeight/2,cx+source.width*shapeWidth/2,cy+source.height*shapeHeight/2),p)}
        OverlayKind.LINE->{p.style=AndroidPaint.Style.STROKE;canvas.drawLine(cx-source.width*shapeWidth/2,cy,cx+source.width*shapeWidth/2,cy,p)}
    }
    canvas.restore()
    return output
}
