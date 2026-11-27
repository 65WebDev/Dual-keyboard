package com.dualkey.ime
import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.os.SystemClock
class DualIme: InputMethodService() {
 private var ru=true; private var shift=0; private var symbols=false; private var lastShift=0L
 private lateinit var root:LinearLayout
 private var navBottom=0
 private val previewWorker=java.util.concurrent.Executors.newSingleThreadExecutor()
 private var previewBusy=false
 private var previewGeneration=0
 private var lastPreviewAt=0L
 private var liveGesture=false
 private fun stopLive(){liveGesture=false;previewGeneration++}
 private fun livePreview(){
 if(!liveGesture||previewBusy||trail.size<2||SystemClock.uptimeMillis()-lastPreviewAt<150)return
 val pos=IntArray(2);root.getLocationOnScreen(pos)
 val centers=swipeKeys.mapValues{(_,v)->val a=IntArray(2);v.getLocationOnScreen(a);android.graphics.PointF(a[0]-pos[0]+v.width/2f,a[1]-pos[1]+v.height/2f)}
 val points=trail.toList();val lang=ru;val caps=shift;val unit=dp(30).toFloat();val generation=previewGeneration
 previewBusy=true;lastPreviewAt=SystemClock.uptimeMillis()
 previewWorker.execute{
 val words=try{decoder.decode(points,centers,lang,unit)}catch(_:Exception){emptyList<String>()}
 ui.post{
 previewBusy=false
 if(liveGesture&&generation==previewGeneration&&isInputViewShown){
 suggestions?.let{bar->bar.removeAllViews();for(word in words){
 val label=if(caps==2)word.uppercase() else if(caps==1)word.replaceFirstChar{it.uppercase()} else word
 bar.addView(TextView(this).apply{text=label;gravity=Gravity.CENTER;textSize=16f;setTextColor(if(p.getBoolean("dark",true))Color.WHITE else Color.BLACK)},LinearLayout.LayoutParams(0,-1,1f))
 }}
 }
 }
 }
 }
 private val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
 private val ui=android.os.Handler(android.os.Looper.getMainLooper())
 private var session=0
 private var touching=false
 private val deferred=ArrayList<()->Unit>()
 private val targets=linkedMapOf<View,String>()
 private var modifier=""
 private var modifierX=0f
 private var modifierY=0f
 private var modifierDrag=false
 private var lineX=0f;private var lineY=0f;private var fingerX=0f;private var fingerY=0f
 private var savedRu=true
 private var savedShift=0
 private var savedAuto=false
 private fun hit(x:Float,y:Float):String?=targets.entries.firstOrNull{(v,_)->val a=IntArray(2);v.getLocationOnScreen(a);x>=a[0]&&x<a[0]+v.width&&y>=a[1]&&y<a[1]+v.height}?.value
 private fun pulse(){if(p.getBoolean("haptic",true)&&!KeyVibration.play(this,p))root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)}
 private fun flush(){val jobs=deferred.toList();deferred.clear();jobs.forEach{it()}}
 private inner class KeyboardRoot:LinearLayout(this@DualIme){
 override fun dispatchDraw(c:android.graphics.Canvas){
 super.dispatchDraw(c)
 if(modifierDrag){val a=IntArray(2);getLocationOnScreen(a);val paint=android.graphics.Paint(3).apply{color=0xff64b5f6.toInt();strokeWidth=dp(5).toFloat();strokeCap=android.graphics.Paint.Cap.ROUND};c.drawLine(lineX-a[0],lineY-a[1],fingerX-a[0],fingerY-a[1],paint)}
 }
 override fun dispatchTouchEvent(e:MotionEvent):Boolean{
 if(e.actionMasked==MotionEvent.ACTION_DOWN){touching=true;modifier=hit(e.rawX,e.rawY).orEmpty().takeIf{it in listOf("⇧","⇪","?123","🌐")}.orEmpty();modifierX=e.rawX;modifierY=e.rawY;modifierDrag=false;targets.entries.firstOrNull{it.value==modifier}?.key?.let{v->val a=IntArray(2);v.getLocationOnScreen(a);lineX=a[0]+v.width/2f;lineY=a[1]+v.height/2f}}
 if(e.actionMasked==MotionEvent.ACTION_MOVE&&modifier.isNotEmpty()){
 if(!modifierDrag&&android.graphics.PointF.length(e.rawX-modifierX,e.rawY-modifierY)>=dp(14)){
 val cancel=MotionEvent.obtain(e);cancel.action=MotionEvent.ACTION_CANCEL;super.dispatchTouchEvent(cancel);cancel.recycle()
 modifierDrag=true;savedShift=shift;savedAuto=autoShift;savedRu=ru
 if(modifier=="?123")symbols=true else if(modifier=="🌐")ru=!ru else shift=1
 draw();pulse()
 }
 if(modifierDrag){fingerX=e.rawX;fingerY=e.rawY;val selected=hit(fingerX,fingerY);targets.forEach{(v,value)->v.isPressed=value==selected&&value.length==1&&value !in listOf("⇧","⇪","⌫","↵")};invalidate();return true}
 }
 if(modifierDrag&&(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL)){
 val value=if(e.actionMasked==MotionEvent.ACTION_UP)hit(e.rawX,e.rawY) else null
 val wasSymbol=modifier=="?123"
 val wasLanguage=modifier=="🌐"
 modifierDrag=false;invalidate();modifier="";touching=false
 symbols=false;ru=savedRu;shift=savedShift;autoShift=savedAuto
 if(value!=null&&value.length==1&&(wasSymbol||wasLanguage||value[0].isLetter())&&value !in listOf("⇧","⇪","⌫","↵","🌐")){
 send(if(wasSymbol||wasLanguage)value else value.uppercase());pulse()
 }
 draw();flush();return true
 }
 val result=super.dispatchTouchEvent(e)
 if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL){touching=false;modifier="";flush()}
 return result
 }
 }

 private var autoShift=false
 private fun wantsCaps():Boolean {
 if(!p.getBoolean("autocaps",true))return false
 val type=currentInputEditorInfo?.inputType?:0
 if((type and 15)!=1||(type and 0xff0) in listOf(0x10,0x20,0x80,0x90,0xd0,0xe0))return false
 val ic=currentInputConnection?:return false
 val before=ic.getTextBeforeCursor(256,0)?.toString()?:return false
 val trimmed=before.trimEnd()
 return before.isEmpty()||trimmed.isEmpty()||before.trimEnd(' ','\t','\r').endsWith('\n')||trimmed.lastOrNull() in listOf('.','!','?','…')
 }
 private fun refreshCaps(){if(shift==2)return;val next=if(wantsCaps())1 else 0;if(shift!=next){shift=next;autoShift=next==1;if(::root.isInitialized)draw()}}

 private val swipeKeys=linkedMapOf<Char,View>()
 private val trail=ArrayList<android.graphics.PointF>()
 private var trailDrawable:android.graphics.drawable.Drawable?=null
 private var suggestions:LinearLayout?=null
 private var lastSwipe=""
 private val decoder by lazy { SwipeDecoder(this) }
 private fun clearTrail(){trail.clear();trailDrawable?.invalidateSelf()}
 private fun swipePoint(x:Float,y:Float){val pos=IntArray(2);root.getLocationOnScreen(pos);val q=android.graphics.PointF(x-pos[0],y-pos[1]);if(trail.isEmpty()||android.graphics.PointF.length(q.x-trail.last().x,q.y-trail.last().y)>=dp(2)){if(trail.size<512)trail.add(q);trailDrawable?.invalidateSelf()}}
 private fun finishSwipe(){
 stopLive();suggestions?.removeAllViews()
 val pos=IntArray(2);root.getLocationOnScreen(pos)
 val centers=swipeKeys.mapValues{(_,v)->val a=IntArray(2);v.getLocationOnScreen(a);android.graphics.PointF(a[0]-pos[0]+v.width/2f,a[1]-pos[1]+v.height/2f)}
 val path=trail.toList();val lang=ru;val unit=dp(30).toFloat();val caps=shift;val token=session
 clearTrail();pulse()
 if(shift==1){shift=0;autoShift=false;ui.post{if(token==session&&!touching)draw()}}
 worker.execute {
 val words=decoder.decode(path,centers,lang,unit)
 ui.post {
 val apply:()->Unit={if(token==session&&isInputViewShown&&words.isNotEmpty())applySwipe(words,caps)}
 apply()
 }
 }
 }
 private fun applySwipe(words:List<String>,caps:Int){
 val options=words.map{if(caps==2)it.uppercase() else if(caps==1)it.replaceFirstChar{c->c.uppercase()} else it}
 val ic=currentInputConnection?:return
 val prev=ic.getTextBeforeCursor(1,0)?.lastOrNull()
 val prefix=if(prev?.isLetterOrDigit()==true)" " else ""
 val inserted=prefix+options.first()+" "
 ic.commitText(inserted,1);lastSwipe=inserted
 if(!liveGesture)suggestions?.let{bar->bar.removeAllViews();for(word in options){bar.addView(TextView(this).apply{text=word;gravity=Gravity.CENTER;textSize=16f;setTextColor(if(p.getBoolean("dark",true))Color.WHITE else Color.BLACK);setOnClickListener{
 pulse();val conn=currentInputConnection
 if(lastSwipe.isNotEmpty()&&conn?.getTextBeforeCursor(lastSwipe.length,0)?.toString()==lastSwipe){conn.deleteSurroundingText(lastSwipe.length,0);conn.commitText(prefix+word+" ",1)}
 lastSwipe="";bar.removeAllViews()
 }},LinearLayout.LayoutParams(0,-1,1f))}}
 }

 private var preview:PopupWindow?=null
 private fun hidePreview(){preview?.dismiss();preview=null}
 override fun onFinishInputView(finishingInput:Boolean){stopLive();session++;deferred.clear();touching=false;modifierDrag=false;clearTrail();lastSwipe="";hidePreview();super.onFinishInputView(finishingInput)}
 private val p by lazy {getSharedPreferences("keys",0)}
 private val redrawSettings=Runnable {if(::root.isInitialized&&isInputViewShown){hidePreview();lastSwipe="";draw();root.requestLayout()}}
 private val settingsListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener {_,key->
 if(key in setOf("height","size","equal","dark","swipe","autocaps")){
 if(::root.isInitialized){root.removeCallbacks(redrawSettings);root.postDelayed(redrawSettings,32L)}
 }
 }
 override fun onCreate(){super.onCreate();p.registerOnSharedPreferenceChangeListener(settingsListener);worker.execute{decoder.warmUp()}}
 override fun onDestroy(){p.unregisterOnSharedPreferenceChangeListener(settingsListener);if(::root.isInitialized)root.removeCallbacks(redrawSettings);stopLive();previewWorker.shutdownNow();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy()}
 private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
 override fun onCreateInputView():View { root=KeyboardRoot().apply { orientation=1 }; root.setOnApplyWindowInsetsListener { v,insets -> navBottom=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom else insets.stableInsetBottom; v.setPadding(dp(2),dp(3),dp(2),dp(4)+navBottom); insets }; draw(); root.requestApplyInsets(); return root }
 override fun onStartInputView(info:EditorInfo?, restarting:Boolean){super.onStartInputView(info,restarting);session++;deferred.clear();ru=p.getBoolean("ru",true);shift=0;autoShift=false;symbols=false;if(wantsCaps()){shift=1;autoShift=true};if(::root.isInitialized)draw()}
 override fun onEvaluateFullscreenMode()=false
 private fun send(s:String){session++;deferred.clear();lastSwipe="";suggestions?.removeAllViews();currentInputConnection?.commitText(s,1);if(shift==1&&s.any{it.isLetter()}){shift=0;autoShift=false;draw()}else if(s.any{it in ".!?…\n"}||s==" "){refreshCaps()}}
 private fun draw(){root.removeAllViews();targets.clear();swipeKeys.clear();clearTrail();trailDrawable?.let{root.overlay.remove(it)}; val dark=p.getBoolean("dark",true);val fg=if(dark)Color.WHITE else Color.rgb(30,33,38);val sub=if(dark)0xffb6c4d8.toInt() else 0xff53647a.toInt();root.setBackgroundColor(if(dark)0xff202124.toInt() else 0xffe1e3e8.toInt());root.setPadding(dp(2),dp(3),dp(2),dp(4)+navBottom)
 val swipeEnabled=p.getBoolean("swipe",true)&&!symbols&&((currentInputEditorInfo?.inputType?:0) and 0xfff) !in listOf(0x81,0x91,0xe1,0x12)
 suggestions=null
 if(swipeEnabled){
 suggestions=LinearLayout(this).also{root.addView(it,LinearLayout.LayoutParams(-1,dp(32)))}
 val d=object:android.graphics.drawable.Drawable(){
 private val paint=android.graphics.Paint(3).apply{color=0xff64b5f6.toInt();strokeWidth=dp(5).toFloat();strokeCap=android.graphics.Paint.Cap.ROUND;strokeJoin=android.graphics.Paint.Join.ROUND;style=android.graphics.Paint.Style.STROKE}
 override fun draw(c:android.graphics.Canvas){if(trail.size<2)return;val path=android.graphics.Path();path.moveTo(trail[0].x,trail[0].y);for(q in trail.drop(1))path.lineTo(q.x,q.y);c.drawPath(path,paint)}
 override fun setAlpha(a:Int){paint.alpha=a}
 override fun setColorFilter(f:android.graphics.ColorFilter?){paint.colorFilter=f}
 override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
 }
 trailDrawable=d;d.setBounds(0,0,root.width,root.height);root.overlay.add(d);root.post{d.setBounds(0,0,root.width,root.height)}
 }
 val h=p.getInt("height",50);val size=p.getInt("size",21).toFloat()
 fun row():LinearLayout=LinearLayout(this).also { root.addView(it,LinearLayout.LayoutParams(-1,dp(h))) }
 fun key(r:LinearLayout, main:String, alt:String="",weight:Float=1f, hold:(()->Unit)?=null, action:()->Unit){val k=LinearLayout(this).apply {orientation=1;gravity=Gravity.CENTER;isClickable=true;contentDescription=if(alt.isEmpty())main else "$main / $alt";background=GradientDrawable().apply {cornerRadius=dp(8).toFloat();setColor(if(dark)0xff3c4043.toInt() else Color.WHITE)} }; val lp=LinearLayout.LayoutParams(0,-1,weight).apply{setMargins(dp(2),dp(3),dp(2),dp(3))
 if(main=="⇧"||main=="⇪"){height=(dp(h)*0.85f).toInt()-dp(6);gravity=Gravity.CENTER_VERTICAL}
 if(main=="↵"){setMargins(dp(2),dp(1),dp(2),dp(1))}};r.addView(k,lp);targets[k]=main
 val digit=main.length==1&&main[0].isDigit()&&alt.isEmpty()
 val letter=alt.isNotEmpty()
 val swipeLetter=swipeEnabled&&letter&&main.length==1&&main[0].isLetter()
 if(swipeLetter)swipeKeys[main[0].lowercaseChar()]=k
 var gesturing=false
 val space=main=="Русский"||main=="English"
 if(letter||digit)k.setPadding(dp(2),dp(2),dp(2),dp(2))
 fun label(s:String,sz:Float,c:Int){
 val t=object:View(this@DualIme){
 private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=c;textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.DEFAULT}
 override fun onDraw(canvas:android.graphics.Canvas){
 super.onDraw(canvas)
 if(width<=0||height<=0)return
 val requested=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,sz,resources.displayMetrics)
 paint.textSize=requested
 val bounds=android.graphics.Rect()
 paint.getTextBounds(s,0,s.length,bounds)
 val metrics=paint.fontMetrics
 val scale=if(letter)minOf(1f,(width-dp(2)).coerceAtLeast(1)/paint.measureText("Ш").coerceAtLeast(1f),(height-dp(2)).coerceAtLeast(1)/(metrics.descent-metrics.ascent).coerceAtLeast(1f)) else minOf(1f,(width-dp(2)).coerceAtLeast(1)/paint.measureText(s).coerceAtLeast(1f),(height-dp(2)).coerceAtLeast(1)/bounds.height().coerceAtLeast(1).toFloat())
 paint.textSize=requested*scale
 paint.getTextBounds(s,0,s.length,bounds)
 val fm=paint.fontMetrics
 canvas.drawText(s,width/2f,if(letter)height/2f-(fm.ascent+fm.descent)/2f else height/2f-(bounds.top+bounds.bottom)/2f,paint)
 }
 }
 if(letter||digit)k.addView(t,LinearLayout.LayoutParams(-1,0,if(letter&&s==alt&&!p.getBoolean("equal",false))0.85f else 1.15f))
 else k.addView(t,LinearLayout.LayoutParams(-1,-1))
 }
 if(main=="↵" || main=="⇧" || main=="⇪"){
 k.addView(object:View(this@DualIme){
 override fun onDraw(c:android.graphics.Canvas){
 super.onDraw(c)
 val scale=minOf(width,height)/32f
 c.save();c.translate(width/2f-16*scale,height/2f-16*scale);c.scale(scale,scale)
 val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=if(main=="↵"||shift>0)0xff17365c.toInt() else fg;strokeWidth=2.8f;strokeCap=android.graphics.Paint.Cap.ROUND;strokeJoin=android.graphics.Paint.Join.ROUND;style=android.graphics.Paint.Style.STROKE}
 val path=android.graphics.Path()
 if(main=="↵"){path.moveTo(27f,7f);path.lineTo(27f,17f);path.lineTo(5f,17f);path.moveTo(13f,9f);path.lineTo(5f,17f);path.lineTo(13f,25f)}
 else {path.moveTo(3f,15f);path.lineTo(16f,3f);path.lineTo(29f,15f);path.lineTo(22f,15f);path.lineTo(22f,25f);path.lineTo(10f,25f);path.lineTo(10f,15f);path.close();if(shift>0)paint.style=android.graphics.Paint.Style.FILL}
 if(main=="⇧")c.translate(0f,2f)
 c.drawPath(path,paint)
 if(main=="⇪"){paint.style=android.graphics.Paint.Style.STROKE;c.drawLine(10f,29f,22f,29f,paint)}
 c.restore()
 }
 },LinearLayout.LayoutParams(dp(if(main=="↵")29 else 25),dp(if(main=="↵")29 else 25)))
 }else if(main=="⌫"){k.addView(object:View(this@DualIme){
 override fun onDraw(c:android.graphics.Canvas){super.onDraw(c);val scale=minOf(width,height)/32f;c.save();c.translate(width/2f-16*scale,height/2f-16*scale);c.scale(scale,scale)
 val paint=android.graphics.Paint(3).apply{color=fg;style=android.graphics.Paint.Style.STROKE;strokeWidth=2.3f;strokeJoin=android.graphics.Paint.Join.ROUND;strokeCap=android.graphics.Paint.Cap.ROUND}
 val path=android.graphics.Path().apply{moveTo(3f,16f);lineTo(11f,7f);lineTo(29f,7f);lineTo(29f,25f);lineTo(11f,25f);close()};c.drawPath(path,paint);c.drawLine(15f,12f,23f,20f,paint);c.drawLine(23f,12f,15f,20f,paint);c.restore()
 }
 },LinearLayout.LayoutParams(dp(27),dp(27)))
 }else if(main=="🌐"){k.addView(object:View(this@DualIme){
 override fun onDraw(c:android.graphics.Canvas){super.onDraw(c);val p=android.graphics.Paint(3).apply{color=fg;style=android.graphics.Paint.Style.STROKE;strokeWidth=dp(2).toFloat()};val x=width/2f;val y=height/2f;val a=minOf(width,height)*0.40f;c.drawCircle(x,y,a,p);c.drawOval(x-a*0.46f,y-a,x+a*0.46f,y+a,p);c.drawLine(x-a,y,x+a,y,p);c.drawLine(x-a*0.85f,y-a*0.5f,x+a*0.85f,y-a*0.5f,p);c.drawLine(x-a*0.85f,y+a*0.5f,x+a*0.85f,y+a*0.5f,p)}
 },LinearLayout.LayoutParams(dp(27),dp(27)))}else{if(alt.isNotEmpty())label(alt,if(p.getBoolean("equal",false))size else size*0.58f,sub);label(main,if(digit)size*1.035f else if(main=="ABC")size*0.75f else if(main.length>3)size*0.78f else size,fg)}
 val accent=main=="↵"
 val activeShift=(main=="⇧"||main=="⇪")&&shift>0
 val special=main in listOf("⇧","⇪","?123","ABC","🌐","⌫")
 val normal=when {accent||activeShift->if(dark)0xffa8c7fa.toInt() else 0xffc2e7ff.toInt();special->if(dark)0xff505764.toInt() else 0xffcbd3e1.toInt();else->if(dark)0xff3c4043.toInt() else Color.WHITE}
 if(accent){(k.getChildAt(k.childCount-1) as? TextView)?.setTextColor(0xff202124.toInt())}
 fun bg(c:Int)=GradientDrawable().apply {cornerRadius=dp(if(accent||main=="?123"||main=="ABC") 30 else 6).toFloat();setColor(c)}
 k.background=android.graphics.drawable.StateListDrawable().apply {addState(intArrayOf(android.R.attr.state_pressed),bg(if(dark)0xff657da5.toInt() else 0xffa8c7fa.toInt()));addState(intArrayOf(),bg(normal))}
 var down=false;var longDone=false
 var sliding=false;var startX=0f;var startY=0f;var cursorX=0f
 fun confirmHold(){if(p.getBoolean("haptic",true)&&!KeyVibration.play(this,p,true))k.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)}
 fun microTick(){if(p.getBoolean("haptic",true))KeyVibration.play(this,p,micro=true)}
 var repeats=0
 val longTask=object:Runnable { override fun run(){
 if(!down)return
 if(main=="⌫"){if(repeats==0)microTick();longDone=true;action();repeats++;k.postDelayed(this,if(repeats>12)45L else if(repeats>5)75L else 120L)}
 else if(hold!=null){longDone=true;hidePreview();if(main=="Русский"||main=="English")confirmHold();hold()}
 } }
 k.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
 override fun onViewAttachedToWindow(v:View){}
 override fun onViewDetachedFromWindow(v:View){down=false;v.removeCallbacks(longTask)}
 })
 k.setOnTouchListener { v,e ->
 when(e.actionMasked){
 MotionEvent.ACTION_DOWN -> {down=true;longDone=false;sliding=false;gesturing=false;if(swipeLetter){stopLive();clearTrail();swipePoint(e.rawX,e.rawY)};startX=e.rawX;startY=e.rawY;cursorX=startX;repeats=0;v.isPressed=true;if(p.getBoolean("haptic",true)){if(!KeyVibration.play(this,p))v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)};if(main=="⌫")v.postDelayed(longTask,350L) else if(hold!=null)v.postDelayed(longTask,ViewConfiguration.getLongPressTimeout().toLong())
 if(p.getBoolean("preview",true)&&main.length==1&&(main[0].isLetterOrDigit()||alt.isNotEmpty())){
 hidePreview();val t=TextView(this).apply{text=main;textSize=32f;gravity=Gravity.CENTER;setTextColor(fg);background=bg(normal)}
 val w=maxOf(dp(48),v.width);val popup=PopupWindow(t,w,dp(64),false).apply{isTouchable=false;isClippingEnabled=true;elevation=dp(6).toFloat()};preview=popup;try { popup.showAsDropDown(v,(v.width-w)/2,-v.height-dp(66)) } catch (_:RuntimeException){hidePreview()}
 }}
 MotionEvent.ACTION_MOVE -> {
 if(swipeLetter&&down&&(!longDone||gesturing)){
 val distance=android.graphics.PointF.length(e.rawX-startX,e.rawY-startY)
 if(!gesturing&&distance>=dp(14)){gesturing=true;liveGesture=true;previewGeneration++;lastPreviewAt=0L;lastSwipe="";suggestions?.removeAllViews();longDone=true;v.removeCallbacks(longTask);hidePreview();v.isPressed=false;pulse()}
 if(gesturing){for(i in 0 until e.historySize)swipePoint(e.getHistoricalX(i)+e.rawX-e.x,e.getHistoricalY(i)+e.rawY-e.y);swipePoint(e.rawX,e.rawY);livePreview()}
 }

 if(space&&down&&(!longDone||sliding)){
 val dx=e.rawX-startX
 if(!sliding&&kotlin.math.abs(dx)>=dp(6)&&kotlin.math.abs(dx)>kotlin.math.abs(e.rawY-startY)){
 sliding=true;longDone=true;v.removeCallbacks(longTask);hidePreview()
 }
 if(sliding){
 val step=(7.5f*resources.displayMetrics.density).coerceAtLeast(1f)
 val count=((e.rawX-cursorX)/step).toInt().coerceIn(-40,40)
 if(count!=0){
 val ic=currentInputConnection
 if(ic!=null){
 val extracted=ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(),0)
 if(extracted!=null&&extracted.selectionStart>=0&&extracted.selectionEnd>=0){
 val content=extracted.text?.toString().orEmpty()
 var pos=(if(count<0)minOf(extracted.selectionStart,extracted.selectionEnd) else maxOf(extracted.selectionStart,extracted.selectionEnd)).coerceIn(0,content.length)
 val initial=pos
 repeat(kotlin.math.abs(count)){
 if(count<0&&pos>0)pos=content.offsetByCodePoints(pos,-1)
 else if(count>0&&pos<content.length)pos=content.offsetByCodePoints(pos,1)
 }
 if(pos!=initial&&ic.setSelection(extracted.startOffset+pos,extracted.startOffset+pos))repeat(kotlin.math.abs(content.codePointCount(minOf(initial,pos),maxOf(initial,pos)))){microTick()}
 }
 }
 cursorX+=count*step
 }
 }
 }
 }
 MotionEvent.ACTION_UP -> {val click=down&&!longDone;if(down&&main=="⌫"&&repeats>0)microTick();down=false;v.removeCallbacks(longTask);hidePreview();v.isPressed=false;if(gesturing){swipePoint(e.rawX,e.rawY);finishSwipe()}else{if(swipeLetter)clearTrail();if(click)v.performClick()}}
 MotionEvent.ACTION_CANCEL -> {if(swipeLetter){stopLive();clearTrail();if(gesturing)suggestions?.removeAllViews()};down=false;v.removeCallbacks(longTask);hidePreview();v.isPressed=false}
 };true }
 k.setOnClickListener {action()}
 
 }
 fun shifted(s:String)=if(shift>0)s.uppercase() else s
 fun shiftKey(r:LinearLayout){key(r,if(shift==2)"⇪" else "⇧",weight=1.25f){val now=SystemClock.uptimeMillis();shift=if(shift==1&&!autoShift&&now-lastShift<400)2 else if(shift>0)0 else 1;autoShift=false;lastShift=now;draw()}}
 fun deleteKey(r:LinearLayout){key(r,"⌫",weight=1.25f){session++;deferred.clear();lastSwipe="";suggestions?.removeAllViews();val ic=currentInputConnection;if(!ic?.getSelectedText(0).isNullOrEmpty())ic?.commitText("",1) else ic?.deleteSurroundingTextInCodePoints(1,0)}}
 val digits=row();for(c in "1234567890")key(digits,c.toString()){send(c.toString())}
 if(symbols){for(line in listOf("!@#\$%^&*()","-_=+[]{}\\|","~`<>:;\"'/?")){val r=row();for(c in line)key(r,c.toString()){send(c.toString())};if(line.startsWith("~"))deleteKey(r)}}else{
 val en=listOf("qwertyuiop[","asdfghjkl;'","zxcvbnm,.");val rus=listOf("йцукенгшщзх","фывапролджэ","ячсмитьбю")
 for(i in en.indices){val r=row();if(i==2)shiftKey(r)
 for(j in en[i].indices){val a=en[i][j].toString();val b=rus[i][j].toString();val m=if(ru)b else a;val n=if(ru)a else b
 val hold:(()->Unit)?=when(b){"е"->if(ru) ({send(shifted("ё"))}) else null;"х"->({send(shifted(if(ru)"ъ" else "]"))});else->null}
 key(r,shifted(m),shifted(n),hold=hold){send(shifted(m))}}
 if(i==2)deleteKey(r)}
 }
 val r=row();key(r,if(symbols)"ABC" else "?123",weight=1.5f){symbols=!symbols;draw()};key(r,",",weight=1f){send(",")};key(r,"🌐",weight=1f){ru=!ru;p.edit().putBoolean("ru",ru).apply();draw()};key(r,if(ru)"Русский" else "English",weight=3.9f,hold={getSystemService(android.view.inputmethod.InputMethodManager::class.java).showInputMethodPicker()}){send(" ")};key(r,".",weight=1f){send(".")};key(r,"↵",weight=2.0f){val a=currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)?:0;if(a in 2..7 && ((currentInputEditorInfo?.imeOptions?:0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0)currentInputConnection?.performEditorAction(a) else send("\n")}
 }
}