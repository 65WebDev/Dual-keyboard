package com.dualkey.ime
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
object KeyVibration {
 @Suppress("DEPRECATION")
 fun play(context:Context,p:SharedPreferences) {
  if(!p.getBoolean("haptic",true)) return
  val v=if(Build.VERSION.SDK_INT>=31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator else context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  if(v==null || !v.hasVibrator()) return
  val duration=p.getInt("vibration_duration",15).coerceIn(5,50).toLong()
  val strength=p.getInt("vibration_strength",30).coerceIn(1,100)
  val amplitude=if(v.hasAmplitudeControl()) (strength*255/100).coerceIn(1,255) else VibrationEffect.DEFAULT_AMPLITUDE
  v.vibrate(VibrationEffect.createOneShot(duration,amplitude))
 }
}