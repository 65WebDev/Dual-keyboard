package com.dualkey.ime
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*
class SettingsActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { val p=getSharedPreferences("keys",0)
 val codes=listOf("system","en")+Languages.all.map{it.code}
 val chosen=p.getString("interface_language","system")?:"system"
 val locale=if(chosen=="system")android.content.res.Resources.getSystem().configuration.locales[0] else java.util.Locale.forLanguageTag(chosen)
 val config=android.content.res.Configuration(resources.configuration).apply{setLocale(locale)}
 val localized=createConfigurationContext(config)
 fun tr(id:Int)=localized.getString(id)
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
 l.addView(TextView(this).apply { text="DualKey 0.36 — EN +"; textSize=24f })
 l.addView(TextView(this).apply{text=tr(R.string.settings_interface);textSize=18f})
 val interfacePicker=Spinner(this)
 interfacePicker.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf(tr(R.string.settings_system),"English")+Languages.all.map{it.name})
 interfacePicker.setSelection(codes.indexOf(chosen).coerceAtLeast(0))
 l.addView(interfacePicker)
 interfacePicker.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
 override fun onNothingSelected(parent:AdapterView<*>?){}
 override fun onItemSelected(parent:AdapterView<*>?,view:android.view.View?,position:Int,id:Long){
 val code=codes[position]
 if(code!=chosen){p.edit().putString("interface_language",code).apply();recreate()}
 }
 }
 button(tr(R.string.settings_enable)) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
 button(tr(R.string.settings_choose)) { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
 fun toggle(s:String,k:String,d:Boolean) { l.addView(Switch(this).apply { text=s; if(night){thumbTintList=android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf()),intArrayOf(0xffa8c7fa.toInt(),0xffa0a0a0.toInt()))}; isChecked=p.getBoolean(k,d); setOnCheckedChangeListener { _,v->p.edit().putBoolean(k,v).apply() } }) }
 l.addView(TextView(this).apply { text=tr(R.string.settings_theme); textSize=18f })
 val themes=RadioGroup(this).apply { orientation=RadioGroup.HORIZONTAL }
 val light=RadioButton(this).apply { id=android.view.View.generateViewId(); text=tr(R.string.settings_light) }
 val dark=RadioButton(this).apply { id=android.view.View.generateViewId(); text=tr(R.string.settings_dark) }
 light.buttonTintList=accent;dark.buttonTintList=accent;themes.addView(light);themes.addView(dark);l.addView(themes)
 themes.check(if(p.getBoolean("dark",true))dark.id else light.id)
 themes.setOnCheckedChangeListener { _,id -> val selected=id==dark.id; if(selected!=night){p.edit().putBoolean("dark",selected).apply();recreate()} }
  toggle(tr(R.string.settings_caps),"autocaps",true); toggle(tr(R.string.settings_swipe),"swipe",true); toggle(tr(R.string.settings_vibration),"haptic",true); toggle(tr(R.string.settings_preview),"preview",true); toggle(tr(R.string.settings_equal),"equal",false)
 l.addView(TextView(this).apply{text=tr(R.string.settings_second);textSize=18f})
 val languages=Spinner(this)
 languages.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,Languages.all.map{it.name})
 languages.setSelection(Languages.all.indexOf(Languages.selected(this)))
 l.addView(languages)
 val status=TextView(this);l.addView(status)
 val download=Button(this).apply{text=tr(R.string.settings_download)};l.addView(download)
 fun updateDictionary(){val lang=Languages.selected(this);val ready=Languages.ready(this,lang.code);status.text=if(ready)tr(R.string.settings_ready) else tr(R.string.settings_missing);download.isEnabled=!ready}
 languages.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
 override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
 override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long){p.edit().putString("language",Languages.all[position].code).apply();updateDictionary()}
 }
 download.setOnClickListener{val code=Languages.selected(this).code;download.isEnabled=false;status.text=tr(R.string.settings_loading);Thread{val result=runCatching{Languages.download(this,code)};runOnUiThread{updateDictionary();if(result.isFailure)status.text=tr(R.string.settings_error)}}.start()}
 l.addView(TextView(this).apply{text=tr(R.string.settings_source)+": FrequencyWords (OpenSubtitles), CC BY-SA 4.0. github.com/hermitdave/FrequencyWords";textSize=12f})
 updateDictionary()
 fun slider(s:String,k:String,min:Int,max:Int,d:Int) { val label=TextView(this); l.addView(label); val bar=SeekBar(this);bar.progressTintList=accent;bar.thumbTintList=accent; bar.max=max-min; bar.progress=p.getInt(k,d)-min; label.text="$s: ${bar.progress+min}"; l.addView(bar); bar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(b:SeekBar,v:Int,u:Boolean){label.text="$s: ${v+min}";p.edit().putInt(k,v+min).apply()}; override fun onStartTrackingTouch(b:SeekBar){};override fun onStopTrackingTouch(b:SeekBar){} }) }
 slider(tr(R.string.settings_strength),"vibration_strength",1,100,75); slider(tr(R.string.settings_duration),"vibration_duration",5,50,30)
 button(tr(R.string.settings_test)) { if(p.getBoolean("haptic",true) && !KeyVibration.play(this,p)) l.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }
 slider(tr(R.string.settings_height),"height",38,70,50); slider(tr(R.string.settings_size),"size",12,26,21)
 l.addView(TextView(this).apply{text=tr(R.string.settings_note);textSize=13f})
 val editor=EditText(this).apply { id=701; hint=tr(R.string.settings_editor); minLines=2; backgroundTintList=accent }
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