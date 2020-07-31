package wayzer.lib

import cf.wayzer.script_agent.Config
import coreLibrary.lib.dataDirectory
import java.io.File
import java.util.*

@Suppress("unused","MemberVisibilityCanBePrivate")
object LangApi{
    const val DEFAULT = "default"
    const val COMMENT = """
        |# Auto generated(自动生成的文件)
        |# backup before modify(修改前注意备份)
        |
    """
    class Lang(val lang:String):Properties(){
        val file: File get() = Config.dataDirectory.resolve("lang").resolve("$lang.properties")
        init {
            if (file.exists())file.reader().use(this::load)
        }
        fun save() {
            file.parentFile.mkdirs()
            file.writer().use {
                it.write(COMMENT.trimMargin())
                store(it, null)
            }
        }
        fun trans(origin: String):String = getProperty(origin.hashCode().toString())?:let{
            put("HASH" + origin.hashCode().toString(), origin)
            save()
            origin
        }
    }
    private val cache = mutableMapOf<String,Lang>()
    fun getLang(lang:String) = cache.getOrPut(lang){Lang(lang)}
    fun clearCache() = cache.clear()
}