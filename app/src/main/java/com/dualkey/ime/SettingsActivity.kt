package com.dualkey.ime
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*
class SettingsActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
 val p=getSharedPreferences("keys",0)
 val l=LinearLayout(this).apply { orientation=1; setPadding(24,64,24,24) }
 fun button(s:String, f:()->Unit) { l.addView(Button(this).apply { text=s; setOnClickListener { f() } }) }
 l.addView(TextView(this).apply { text="DualKey 0.5 — RU / EN"; textSize=24f })
 button("1. Включить клавиатуру") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
 button("2. Выбрать DualKey") { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
 fun toggle(s:String,k:String,d:Boolean) { l.addView(Switch(this).apply { text=s; isChecked=p.getBoolean(k,d); setOnCheckedChangeListener { _,v->p.edit().putBoolean(k,v).apply() } }) }
 l.addView(TextView(this).apply { text="Тема клавиатуры"; textSize=18f })
 val themes=RadioGroup(this).apply { orientation=RadioGroup.HORIZONTAL }
 val light=RadioButton(this).apply { id=android.view.View.generateViewId(); text="Светлая" }
 val dark=RadioButton(this).apply { id=android.view.View.generateViewId(); text="Тёмная (Gboard)" }
 themes.addView(light);themes.addView(dark);l.addView(themes)
 themes.check(if(p.getBoolean("dark",true))dark.id else light.id)
 themes.setOnCheckedChangeListener { _,id -> p.edit().putBoolean("dark",id==dark.id).apply() }
  toggle("Вибрация","haptic",true); toggle("Всплывающая буква","preview",true); toggle("Одинаковый размер букв","equal",false)
 fun slider(s:String,k:String,min:Int,max:Int,d:Int) { val label=TextView(this); l.addView(label); val bar=SeekBar(this); bar.max=max-min; bar.progress=p.getInt(k,d)-min; label.text="$s: ${bar.progress+min}"; l.addView(bar); bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(b:SeekBar,v:Int,u:Boolean){label.text="$s: ${v+min}";p.edit().putInt(k,v+min).apply()}; override fun onStartTrackingTouch(b:SeekBar){};override fun onStopTrackingTouch(b:SeekBar){} }) }
 slider("Интенсивность вибрации, %","vibration_strength",1,100,30); slider("Длительность вибрации, мс","vibration_duration",5,50,15)
 button("Проверить вибрацию") { if(p.getBoolean("haptic",true)) KeyVibration.play(this,p) }
 slider("Высота клавиши","height",38,70,50); slider("Размер букв","size",16,26,21)
 l.addView(EditText(this).apply { hint="Проверка ввода"; minLines=2 }); setContentView(ScrollView(this).apply { addView(l) })
 }
}