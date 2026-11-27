package com.dualkey.ime
import android.content.Context
object Languages {
 data class Layout(val code:String,val name:String,val rows:List<String>)
 val all=listOf(Layout("ru","Русский",listOf("йцукенгшщзх","фывапролджэ","ячсмитьбю")),
Layout("uk","Українська",listOf("йцукенгшщзх","фівапролджє","ячсмитьбю")),
Layout("de","Deutsch",listOf("qwertzuiopü","asdfghjklöä","yxcvbnm,.")),
Layout("fr","Français",listOf("azertyuiop^","qsdfghjklmù","wxcvbn,;:")),
Layout("es","Español",listOf("qwertyuiop[","asdfghjklñ´","zxcvbnm,.")),
Layout("it","Italiano",listOf("qwertyuiopè","asdfghjklòà","zxcvbnm,.")),
Layout("pt","Português",listOf("qwertyuiop´","asdfghjklçã","zxcvbnm,.")),
Layout("pl","Polski",listOf("qwertyuiop[","asdfghjkl;'","zxcvbnm,.")),
Layout("tr","Türkçe",listOf("qwertyuıopğ","asdfghjklşi","zxcvbnmöç")),
Layout("el","Ελληνικά",listOf("ςερτυθιοπ[]","ασδφγηξκλ;'","ζχψωβνμ,.")))
 fun selected(c:Context)=all.firstOrNull{it.code==c.getSharedPreferences("keys",0).getString("language","ru")}?:all.first()
 fun file(c:Context,code:String)=java.io.File(c.filesDir,"swipe_"+code+".txt")
 fun ready(c:Context,code:String)=code in listOf("ru","en")||file(c,code).isFile
 fun download(c:Context,code:String){
 require(all.any{it.code==code}&&code!="ru")
 val target=file(c,code);val temp=java.io.File(c.filesDir,"swipe_"+code+".tmp")
 val url=java.net.URL("https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/"+code+"/"+code+"_50k.txt")
 val conn=url.openConnection() as java.net.HttpURLConnection
 conn.connectTimeout=15000;conn.readTimeout=20000
 try{
 require(conn.responseCode==200)
 var count=0
 conn.inputStream.bufferedReader(Charsets.UTF_8).use{reader->temp.bufferedWriter(Charsets.UTF_8).use{out->reader.forEachLine{line->
 val word=line.substringBefore(' ').lowercase(java.util.Locale.forLanguageTag(code))
 if(word.length in 2..30&&word.all{it.isLetter()}&&count<50000){out.write(word);out.newLine();count++}
 }}}
 require(count>=1000);check(temp.renameTo(target))
 }finally{conn.disconnect();temp.delete()}
 }
 fun variants(code:String,c:Char):String=when(code){
 "ru"->when(c){'е'->"ё";'х'->"ъ";else->""}
 "uk"->when(c){'г'->"ґ";'и'->"ї";else->""}
 "de"->if(c=='s')"ß" else ""
 "pl"->when(c){'a'->"ą";'c'->"ć";'e'->"ę";'l'->"ł";'n'->"ń";'o'->"ó";'s'->"ś";'z'->"żź";else->""}
 "es"->when(c){'a'->"á";'e'->"é";'i'->"í";'o'->"ó";'u'->"úü";else->""}
 "fr","it","pt"->when(c){'a'->"àáâãä";'e'->"éèêë";'i'->"íìîï";'o'->"óòôõö";'u'->"úùûü";'c'->"ç";else->""}
 "el"->when(c){'α'->"ά";'ε'->"έ";'η'->"ή";'ι'->"ίϊΐ";'ο'->"ό";'υ'->"ύϋΰ";'ω'->"ώ";else->""}
 else->""
 }
}
