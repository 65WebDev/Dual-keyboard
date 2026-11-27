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
 private var preview:PopupWindow?=null
 private fun hidePreview(){preview?.dismiss();preview=null}
 override fun onFinishInputView(finishingInput:Boolean){hidePreview();super.onFinishInputView(finishingInput)}
 private val p get()=getSharedPreferences("keys",0)
 private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
 override fun onCreateInputView():View { root=LinearLayout(this).apply { orientation=1 }; root.setOnApplyWindowInsetsListener { v,insets -> navBottom=if(android.os.Build.VERSION.SDK_INT>=30) insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom else insets.stableInsetBottom; v.setPadding(dp(2),dp(3),dp(2),dp(4)+navBottom); insets }; draw(); root.requestApplyInsets(); return root }
 override fun onStartInputView(info:EditorInfo?, restarting:Boolean){super.onStartInputView(info,restarting);ru=p.getBoolean("ru",true);shift=0;symbols=false;if(::root.isInitialized)draw()}
 override fun onEvaluateFullscreenMode()=false
 private fun send(s:String){currentInputConnection?.commitText(s,1);if(shift==1){shift=0;draw()}}
 private fun draw(){root.removeAllViews(); val dark=p.getBoolean("dark",true);val fg=if(dark)Color.WHITE else Color.rgb(30,33,38);val sub=if(dark)0xffb6c4d8.toInt() else 0xff53647a.toInt();root.setBackgroundColor(if(dark)0xff202124.toInt() else 0xffe1e3e8.toInt());root.setPadding(dp(2),dp(3),dp(2),dp(4)+navBottom)
 val h=p.getInt("height",50);val size=p.getInt("size",21).toFloat()
 fun row():LinearLayout=LinearLayout(this).also { root.addView(it,LinearLayout.LayoutParams(-1,dp(h))) }
 fun key(r:LinearLayout, main:String, alt:String="",weight:Float=1f, hold:(()->Unit)?=null, action:()->Unit){val k=LinearLayout(this).apply {orientation=1;gravity=Gravity.CENTER;isClickable=true;contentDescription=if(alt.isEmpty())main else "$main / $alt";background=GradientDrawable().apply {cornerRadius=dp(8).toFloat();setColor(if(dark)0xff3c4043.toInt() else Color.WHITE)} }; val lp=LinearLayout.LayoutParams(0,-1,weight).apply{setMargins(dp(2),dp(3),dp(2),dp(3))
 if(main=="⇧"||main=="⇪"){height=(dp(h)*0.85f).toInt()-dp(6);gravity=Gravity.CENTER_VERTICAL}
 if(main=="↵"){setMargins(dp(2),dp(1),dp(2),dp(1))}};r.addView(k,lp)
 fun label(s:String,sz:Float,c:Int){k.addView(TextView(this).apply{text=s;textSize=sz;setTextColor(c);gravity=Gravity.CENTER;includeFontPadding=false;maxLines=1})}
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
 c.drawPath(path,paint)
 if(main=="⇪"){paint.style=android.graphics.Paint.Style.STROKE;c.drawLine(10f,29f,22f,29f,paint)}
 c.restore()
 }
 },LinearLayout.LayoutParams(dp(if(main=="↵")32 else 25),dp(if(main=="↵")32 else 25)))
 }else if(main=="🌐"){k.addView(object:View(this@DualIme){
 override fun onDraw(c:android.graphics.Canvas){super.onDraw(c);val p=android.graphics.Paint(3).apply{color=fg;style=android.graphics.Paint.Style.STROKE;strokeWidth=dp(2).toFloat()};val x=width/2f;val y=height/2f;val a=minOf(width,height)*0.40f;c.drawCircle(x,y,a,p);c.drawOval(x-a*0.46f,y-a,x+a*0.46f,y+a,p);c.drawLine(x-a,y,x+a,y,p);c.drawLine(x-a*0.85f,y-a*0.5f,x+a*0.85f,y-a*0.5f,p);c.drawLine(x-a*0.85f,y+a*0.5f,x+a*0.85f,y+a*0.5f,p)}
 },LinearLayout.LayoutParams(dp(27),dp(27)))}else{if(alt.isNotEmpty())label(alt,if(p.getBoolean("equal",false))size else size*0.58f,sub);label(main,if(main.length>3)size*0.78f else size,fg)}
 val accent=main=="↵"
 val activeShift=(main=="⇧"||main=="⇪")&&shift>0
 val special=main in listOf("⇧","⇪","?123","ABC","🌐","⌫")
 val normal=when {accent||activeShift->if(dark)0xffa8c7fa.toInt() else 0xffc2e7ff.toInt();special->if(dark)0xff505764.toInt() else 0xffcbd3e1.toInt();else->if(dark)0xff3c4043.toInt() else Color.WHITE}
 if(accent){(k.getChildAt(k.childCount-1) as? TextView)?.setTextColor(0xff202124.toInt())}
 fun bg(c:Int)=GradientDrawable().apply {cornerRadius=dp(if(accent||main=="?123"||main=="ABC") 30 else 6).toFloat();setColor(c)}
 k.background=android.graphics.drawable.StateListDrawable().apply {addState(intArrayOf(android.R.attr.state_pressed),bg(if(dark)0xff657da5.toInt() else 0xffa8c7fa.toInt()));addState(intArrayOf(),bg(normal))}
 var down=false;var longDone=false
 fun confirmHold(){if(p.getBoolean("haptic",true)&&!KeyVibration.play(this,p,true))k.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)}
 var repeats=0
 val longTask=object:Runnable { override fun run(){
 if(!down)return
 if(main=="⌫"){if(repeats==0)confirmHold();longDone=true;action();repeats++;k.postDelayed(this,if(repeats>12)45L else if(repeats>5)75L else 120L)}
 else if(hold!=null){longDone=true;hidePreview();if(main=="Русский"||main=="English")confirmHold();hold()}
 } }
 k.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
 override fun onViewAttachedToWindow(v:View){}
 override fun onViewDetachedFromWindow(v:View){down=false;v.removeCallbacks(longTask)}
 })
 k.setOnTouchListener { v,e ->
 when(e.actionMasked){
 MotionEvent.ACTION_DOWN -> {down=true;longDone=false;repeats=0;v.isPressed=true;if(p.getBoolean("haptic",true)){if(!KeyVibration.play(this,p))v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)};if(main=="⌫")v.postDelayed(longTask,350L) else if(hold!=null)v.postDelayed(longTask,ViewConfiguration.getLongPressTimeout().toLong())
 if(p.getBoolean("preview",true)&&main.length==1&&(main[0].isLetterOrDigit()||alt.isNotEmpty())){
 hidePreview();val t=TextView(this).apply{text=main;textSize=32f;gravity=Gravity.CENTER;setTextColor(fg);background=bg(normal)}
 val w=maxOf(dp(48),v.width);val popup=PopupWindow(t,w,dp(64),false).apply{isTouchable=false;isClippingEnabled=true;elevation=dp(6).toFloat()};preview=popup;try { popup.showAsDropDown(v,(v.width-w)/2,-v.height-dp(66)) } catch (_:RuntimeException){hidePreview()}
 }}
 MotionEvent.ACTION_UP -> {val click=down&&!longDone;down=false;v.removeCallbacks(longTask);hidePreview();v.isPressed=false;if(click)v.performClick()}
 MotionEvent.ACTION_CANCEL -> {down=false;v.removeCallbacks(longTask);hidePreview();v.isPressed=false}
 };true }
 k.setOnClickListener {action()}
 
 }
 fun shifted(s:String)=if(shift>0)s.uppercase() else s
 fun shiftKey(r:LinearLayout){key(r,if(shift==2)"⇪" else "⇧",weight=1.25f){val now=SystemClock.uptimeMillis();shift=if(shift==1&&now-lastShift<400)2 else if(shift>0)0 else 1;lastShift=now;draw()}}
 fun deleteKey(r:LinearLayout){key(r,"⌫",weight=1.25f){val ic=currentInputConnection;if(!ic?.getSelectedText(0).isNullOrEmpty())ic?.commitText("",1) else ic?.deleteSurroundingTextInCodePoints(1,0)}}
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