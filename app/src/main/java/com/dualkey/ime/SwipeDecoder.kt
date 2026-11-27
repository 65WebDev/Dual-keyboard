package com.dualkey.ime
import android.content.Context
import android.graphics.PointF
import kotlin.math.*
class SwipeDecoder(private val context:Context){
 private val cache=mutableMapOf<String,Pair<Long,Map<Pair<Char,Char>,List<IndexedValue<String>>>>>()
 @Synchronized private fun dictionary(lang:String):Map<Pair<Char,Char>,List<IndexedValue<String>>>{
 val stamp=Languages.file(context,lang).lastModified()
 cache[lang]?.let{if(it.first==stamp)return it.second}
 val data=index(load(lang),lang);cache[lang]=stamp to data;return data
 }
 fun warmUp(){dictionary("en");dictionary(Languages.selected(context).code)}
 private fun load(lang:String):List<String> = try{(if(lang in listOf("en","ru"))context.assets.open("swipe_$lang.txt") else Languages.file(context,lang).inputStream()).bufferedReader().useLines{lines->lines.flatMap{it.split(Regex("\\s+")).asSequence()}.filter{it.length>=2&&it.all(Char::isLetter)}.distinct().toList()}}catch(e:Exception){emptyList()}
 private fun normalized(c:Char,lang:String):Char {if(lang=="ru")return when(c){'ё'->'е';'ъ'->'х';else->c};return c}
 private fun index(words:List<String>,lang:String)=words.withIndex().groupBy{normalized(it.value.first(),lang) to normalized(it.value.last(),lang)}
 private fun dist(a:PointF,b:PointF)=hypot(a.x-b.x,a.y-b.y)
 private fun sample(p:List<PointF>,n:Int=24):List<PointF>{
 if(p.size<2)return List(n){p.first()}
 val lengths=FloatArray(p.size);for(i in 1 until p.size)lengths[i]=lengths[i-1]+dist(p[i-1],p[i])
 if(lengths.last()<0.01f)return List(n){p.first()}
 var j=1
 return List(n){i->val target=lengths.last()*i/(n-1);while(j<p.lastIndex&&lengths[j]<target)j++;val t=((target-lengths[j-1])/(lengths[j]-lengths[j-1]).coerceAtLeast(0.001f)).coerceIn(0f,1f);PointF(p[j-1].x+(p[j].x-p[j-1].x)*t,p[j-1].y+(p[j].y-p[j-1].y)*t)}
 }
 fun decode(path:List<PointF>,keys:Map<Char,PointF>,lang:String,unit:Float):List<String>{
 if(path.size<2||keys.isEmpty())return emptyList()
 val actual=sample(path)
 val first=path.first();val last=path.last()
 val best=java.util.PriorityQueue<Pair<String,Float>>(compareByDescending{it.second})
 val starts=keys.entries.sortedBy{dist(first,it.value)}.take(3).filter{dist(first,it.value)/unit<2.0f}.map{it.key}
 val ends=keys.entries.sortedBy{dist(last,it.value)}.take(3).filter{dist(last,it.value)/unit<2.0f}.map{it.key}
 val dictionary=dictionary(lang)
 for(a in starts)for(b in ends)for((rank,word) in dictionary[a to b].orEmpty()){
 if(dist(first,keys[a]!!)+dist(last,keys[b]!!)>unit*3.2f)continue
 val points=word.mapNotNull{keys[normalized(it,lang)]}
 if(points.size!=word.length)continue
 val endpoint=(dist(first,points.first())+dist(last,points.last()))/unit
 if(endpoint>3.2f)continue
 val expected=sample(points)
 var error=0f;for(i in actual.indices){val dx=(actual[i].x-expected[i].x)/unit;val dy=(actual[i].y-expected[i].y)/unit;error+=dx*dx+dy*dy}
 val score=error/actual.size+endpoint*0.45f+ln(1f+rank)*0.025f
 if(best.size<3||score<best.peek()!!.second){best.add(word to score);if(best.size>3)best.poll()}
 }
 return best.toList().sortedBy{it.second}.filter{it.second<5f}.map{it.first}
 }
}