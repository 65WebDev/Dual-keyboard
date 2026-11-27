package com.dualkey.ime
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
object KeyVibration {
 @Suppress("DEPRECATION")
 fun play(context:Context,p:SharedPreferences):Boolean {
  if(!p.getBoolean("haptic",true)) return false
  val v=if(Build.VERSION.SDK_INT>=31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator else context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  if(v==null || !v.hasVibrator()) return false
  val duration=p.getInt("vibration_duration",15).coerceIn(5,50).toLong()
  val strength=p.getInt("vibration_strength",30).coerceIn(1,100)
  val amplitude=if(v.hasAmplitudeControl()) (strength*255/100).coerceIn(1,255) else VibrationEffect.DEFAULT_AMPLITUDE
  return try { val effect=VibrationEffect.createOneShot(duration,amplitude)
  if(Build.VERSION.SDK_INT>=33) v.vibrate(effect,android.os.VibrationAttributes.Builder().setUsage(android.os.VibrationAttributes.USAGE_TOUCH).build()) else v.vibrate(effect,android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build());true } catch (_:RuntimeException){false}
 }
}