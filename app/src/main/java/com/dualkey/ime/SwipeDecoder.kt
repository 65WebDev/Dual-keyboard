package com.dualkey.ime
import android.content.Context
import android.graphics.PointF
import kotlin.math.*
class SwipeDecoder(private val context:Context){
 private val en by lazy{index(load("en"))}
 private val ru by lazy{index(load("ru"))}
 fun warmUp(){en.size;ru.size}
 private fun load(lang:String):List<String> = try{context.assets.open("swipe_$lang.txt").bufferedReader().useLines{lines->lines.flatMap{it.split(Regex("\\s+")).asSequence()}.filter{it.length>=2&&it.all(Char::isLetter)}.distinct().toList()}}catch(e:Exception){emptyList()}
 private fun normalized(c:Char)=when(c){'ё'->'е';'ъ'->'х';else->c}
 private fun index(words:List<String>)=words.withIndex().groupBy{normalized(it.value.first()) to normalized(it.value.last())}
 private fun dist(a:PointF,b:PointF)=hypot(a.x-b.x,a.y-b.y)
 private fun sample(p:List<PointF>,n:Int=24):List<PointF>{
 if(p.size<2)return List(n){p.first()}
 val lengths=FloatArray(p.size);for(i in 1 until p.size)lengths[i]=lengths[i-1]+dist(p[i-1],p[i])
 if(lengths.last()<0.01f)return List(n){p.first()}
 var j=1
 return List(n){i->val target=lengths.last()*i/(n-1);while(j<p.lastIndex&&lengths[j]<target)j++;val t=((target-lengths[j-1])/(lengths[j]-lengths[j-1]).coerceAtLeast(0.001f)).coerceIn(0f,1f);PointF(p[j-1].x+(p[j].x-p[j-1].x)*t,p[j-1].y+(p[j].y-p[j-1].y)*t)}
 }
 fun decode(path:List<PointF>,keys:Map<Char,PointF>,russian:Boolean,unit:Float):List<String>{
 if(path.size<2||keys.isEmpty())return emptyList()
 val actual=sample(path)
 val first=path.first();val last=path.last()
 val best=java.util.PriorityQueue<Pair<String,Float>>(compareByDescending{it.second})
 val starts=keys.entries.sortedBy{dist(first,it.value)}.take(3).filter{dist(first,it.value)/unit<2.0f}.map{it.key}
 val ends=keys.entries.sortedBy{dist(last,it.value)}.take(3).filter{dist(last,it.value)/unit<2.0f}.map{it.key}
 val dictionary=if(russian)ru else en
 for(a in starts)for(b in ends)for((rank,word) in dictionary[a to b].orEmpty()){
 if(dist(first,keys[a]!!)+dist(last,keys[b]!!)>unit*3.2f)continue
 val points=word.mapNotNull{keys[when(it){'ё'->'е';'ъ'->'х';else->it}]}
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