package com.dualkey.ime
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*
class SettingsActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { val p=getSharedPreferences("keys",0)
 val night=p.getBoolean("dark",true)
 setTheme(if(night) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
 super.onCreate(savedInstanceState)
 val bg=if(night)0xff202124.toInt() else 0xfffafafa.toInt()
 val accent=android.content.res.ColorStateList.valueOf(if(night)0xffa8c7fa.toInt() else 0xff006b60.toInt())
 window.statusBarColor=bg;window.navigationBarColor=bg
 window.decorView.systemUiVisibility=if(night)0 else (android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
 window.decorView.setBackgroundColor(bg)
 window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
 val l=LinearLayout(this).apply { orientation=1; setPadding(24,16,24,24) }
 fun button(s:String, f:()->Unit) { l.addView(Button(this).apply { text=s; setOnClickListener { f() } }) }
 l.addView(TextView(this).apply { text="DualKey 0.24 — RU / EN"; textSize=24f })
 button("1. Включить клавиатуру") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
 button("2. Выбрать DualKey") { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
 fun toggle(s:String,k:String,d:Boolean) { l.addView(Switch(this).apply { text=s; if(night){thumbTintList=android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf()),intArrayOf(0xffa8c7fa.toInt(),0xffa0a0a0.toInt()))}; isChecked=p.getBoolean(k,d); setOnCheckedChangeListener { _,v->p.edit().putBoolean(k,v).apply() } }) }
 l.addView(TextView(this).apply { text="Тема приложения и клавиатуры"; textSize=18f })
 val themes=RadioGroup(this).apply { orientation=RadioGroup.HORIZONTAL }
 val light=RadioButton(this).apply { id=android.view.View.generateViewId(); text="Светлая" }
 val dark=RadioButton(this).apply { id=android.view.View.generateViewId(); text="Тёмная (Gboard)" }
 light.buttonTintList=accent;dark.buttonTintList=accent;themes.addView(light);themes.addView(dark);l.addView(themes)
 themes.check(if(p.getBoolean("dark",true))dark.id else light.id)
 themes.setOnCheckedChangeListener { _,id -> val selected=id==dark.id; if(selected!=night){p.edit().putBoolean("dark",selected).apply();recreate()} }
  toggle("Автоматическая заглавная буква","autocaps",true); toggle("Ввод свайпом (экспериментальный)","swipe",true); toggle("Вибрация","haptic",true); toggle("Всплывающая буква","preview",true); toggle("Одинаковый размер букв","equal",false)
 fun slider(s:String,k:String,min:Int,max:Int,d:Int) { val label=TextView(this); l.addView(label); val bar=SeekBar(this);bar.progressTintList=accent;bar.thumbTintList=accent; bar.max=max-min; bar.progress=p.getInt(k,d)-min; label.text="$s: ${bar.progress+min}"; l.addView(bar); bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(b:SeekBar,v:Int,u:Boolean){label.text="$s: ${v+min}";p.edit().putInt(k,v+min).apply()}; override fun onStartTrackingTouch(b:SeekBar){};override fun onStopTrackingTouch(b:SeekBar){} }) }
 slider("Интенсивность вибрации, %","vibration_strength",1,100,30); slider("Длительность вибрации, мс","vibration_duration",5,50,15)
 button("Проверить вибрацию") { if(p.getBoolean("haptic",true) && !KeyVibration.play(this,p)) l.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }
 slider("Высота ряда клавиш, dp","height",38,70,50); slider("Размер букв, sp","size",12,26,21)
 l.addView(TextView(this).apply{text="Изменения видны сразу в открытой клавиатуре. Буквы автоматически уменьшаются, если не помещаются в клавишу.";textSize=13f})
 val editor=EditText(this).apply { id=701; hint="Проверка ввода"; minLines=2; backgroundTintList=accent }
 val scroll=ScrollView(this).apply { id=702; addView(l); clipToPadding=true }
 val container=LinearLayout(this).apply {
 orientation=1;setBackgroundColor(bg);isFocusableInTouchMode=true
 addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
 addView(editor,LinearLayout.LayoutParams(-1,-2).apply{setMargins(24,0,24,8)})
 }
 if(android.os.Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false)
 setContentView(container)
 container.setOnApplyWindowInsetsListener { v,insets ->
 if(android.os.Build.VERSION.SDK_INT>=30){
 val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
 val ime=insets.getInsets(android.view.WindowInsets.Type.ime())
 v.setPadding(bars.left,bars.top,bars.right,maxOf(bars.bottom,ime.bottom))
 }else{v.setPadding(0,insets.systemWindowInsetTop,0,0)}
 insets
 }
 container.requestApplyInsets()
 container.requestFocus()
 }
}