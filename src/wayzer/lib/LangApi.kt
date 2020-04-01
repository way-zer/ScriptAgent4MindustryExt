package wayzer.lib

import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.placehold.PlaceHoldContext
import mindustry.Vars
import java.io.File
import java.util.*

@Suppress("unused","MemberVisibilityCanBePrivate")
object LangApi{
    const val DEFAULT = "default"
    const val COMMENT = """
        |Auto generated(自动生成的文件)
        |backup before modify(修改前注意备份)
    """
    class Lang(val lang:String):Properties(){
        val file: File get() = Vars.dataDirectory.child("lang").child("$lang.properties").file()
        init {
            if (file.exists())file.reader().use(this::load)
        }
        fun save(){
            file.parentFile.mkdirs()
            file.writer().use { store(it, COMMENT.trimMargin()) }
        }
        fun trans(origin: String):String = getProperty(origin.hashCode().toString())?:let{
            put(origin.hashCode(),origin)
            save()
            origin
        }
    }
    private val cache = mutableMapOf<String,Lang>()
    fun getLang(lang:String) = cache.getOrPut(lang){Lang(lang)}
    fun clearCache() = cache.clear()
}

fun String.i18n(vararg vars:Pair<String,Any> = emptyArray(),lang: String? = null):PlaceHoldContext{
    return LangApi.getLang(lang?:LangApi.DEFAULT).trans(this).with(vars.toMap())
}