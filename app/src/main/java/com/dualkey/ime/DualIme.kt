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
 private fun draw(){root.removeAllViews(); val dark=p.getBoolean("dark",true);val fg=if(dark)Color.WHITE else Color.rgb(30,33,38);val sub=if(dark)0xffb6c4d8.toInt() else 0xff53647a.toInt();root.setBackgroundColor(if(dark)0xff202124.toInt() else 0xffe2e7ef.toInt());root.setPadding(dp(2),dp(3),dp(2),dp(4)+navBottom)
 val h=p.getInt("height",50);val size=p.getInt("size",21).toFloat()
 fun row():LinearLayout=LinearLayout(this).also { root.addView(it,LinearLayout.LayoutParams(-1,dp(h))) }
 fun key(r:LinearLayout, main:String, alt:String="",weight:Float=1f, action:()->Unit){val k=LinearLayout(this).apply {orientation=1;gravity=Gravity.CENTER;isClickable=true;contentDescription=if(alt.isEmpty())main else "$main / $alt";background=GradientDrawable().apply {cornerRadius=dp(6).toFloat();setColor(if(dark)0xff3c4043.toInt() else Color.WHITE)} }; val lp=LinearLayout.LayoutParams(0,-1,weight).apply{setMargins(dp(1),dp(2),dp(1),dp(2))};r.addView(k,lp)
 fun label(s:String,sz:Float,c:Int){k.addView(TextView(this).apply{text=s;textSize=sz;setTextColor(c);gravity=Gravity.CENTER;includeFontPadding=false;maxLines=1})}
 if(alt.isNotEmpty())label(alt,if(p.getBoolean("equal",false))size else size*0.58f,sub);label(main,size,fg)
 val normal=if(dark)0xff3c4043.toInt() else Color.WHITE
 fun bg(c:Int)=GradientDrawable().apply {cornerRadius=dp(6).toFloat();setColor(c)}
 k.background=android.graphics.drawable.StateListDrawable().apply {addState(intArrayOf(android.R.attr.state_pressed),bg(if(dark)0xff758bb0.toInt() else 0xffa8c7fa.toInt()));addState(intArrayOf(),bg(normal))}
 k.setOnTouchListener { v,e ->
 when(e.actionMasked){
 MotionEvent.ACTION_DOWN -> {v.isPressed=true;if(p.getBoolean("haptic",true))v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
 if(p.getBoolean("preview",true)&&main.length==1&&(main[0].isLetterOrDigit()||alt.isNotEmpty())){
 hidePreview();val t=TextView(this).apply{text=main;textSize=32f;gravity=Gravity.CENTER;setTextColor(fg);background=bg(normal)}
 val w=maxOf(dp(48),v.width);val popup=PopupWindow(t,w,dp(64),false).apply{isTouchable=false;isClippingEnabled=true;elevation=dp(6).toFloat()};preview=popup;popup.showAsDropDown(v,(v.width-w)/2,-v.height-dp(66))
 }}
 MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {hidePreview();v.isPressed=false}
 };false }
 k.setOnClickListener {action()}
 }
 val digits=row();for(c in "1234567890")key(digits,c.toString()){send(c.toString())}
 if(symbols){for(line in listOf("!@#\$%^&*()","-_=+[]{}\\|","~`<>:;\"'/?")){val r=row();for(c in line)key(r,c.toString()){send(c.toString())}}}else{
 val en=listOf("qwertyuiop[]","asdfghjkl;'","zxcvbnm,.");val rus=listOf("йцукенгшщзхъ","фывапролджэ","ячсмитьбю")
 for(i in en.indices){val r=row();for(j in en[i].indices){val a=en[i][j].toString();val b=rus[i][j].toString();val m=if(ru)b else a;val n=if(ru)a else b;key(r,if(shift>0)m.uppercase() else m,if(shift>0)n.uppercase() else n){send(if(shift>0)m.uppercase() else m)}}}
 }
 val r=row();key(r,if(shift==2)"⇪" else "⇧"){val now=SystemClock.uptimeMillis();shift=if(shift==1&&now-lastShift<400)2 else if(shift>0)0 else 1;lastShift=now;draw()};key(r,if(symbols)"ABC" else "?123"){symbols=!symbols;draw()};key(r,"🌐"){ru=!ru;p.edit().putBoolean("ru",ru).apply();draw()};key(r,"ё"){send(if(shift>0)"Ё" else "ё")};key(r,if(ru)"Русский" else "English",weight=3f){send(" ")};key(r,"."){send(".")};key(r,"⌫"){val ic=currentInputConnection; if(!ic?.getSelectedText(0).isNullOrEmpty())ic?.commitText("",1) else ic?.deleteSurroundingTextInCodePoints(1,0)};key(r,"↵"){val a=currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)?:0;if(a in 2..7 && ((currentInputEditorInfo?.imeOptions?:0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION)==0)currentInputConnection?.performEditorAction(a) else send("\n")}
 }
}