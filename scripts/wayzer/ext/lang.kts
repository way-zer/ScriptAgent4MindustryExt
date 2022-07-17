package wayzer.ext

import cf.wayzer.placehold.TemplateHandler
import cf.wayzer.placehold.TemplateHandlerKey
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.StringWriter
import java.util.*

name = "国际化多语言"

var default by config.key("default.user", "默认语言")
var console by config.key("default.console", "控制台语言(不发给玩家的语句)")

val tempLang = mutableMapOf<String, String>()//uuid -> lang

var Player.lang
    get() = tempLang[uuid()] ?: PlayerData[uuid()].profile?.lang ?: default
    set(v) {
        if (lang == v) return
        PlayerData[uuid()].secureProfile(this)?.apply {
            launch(Dispatchers.IO) {
                transaction {
                    lang = v
                }
            }
        } ?: let {
            tempLang[uuid()] = v
            sendMessage("[yellow]当前未绑定账,语言设置将在退出游戏后重置".with())
        }
    }

listen<EventType.PlayerLeave> {
    tempLang.remove(it.player.uuid())
}


//===main===
class Lang(private val lang: String) : Properties() {
    private val file: File get() = dir.resolve("$lang.properties")

    init {
        if (file.exists()) file.reader().use(this::load)
    }

    private fun save() {
        file.parentFile.mkdirs()
        file.writer().use {
            it.write(header)
            store(it, null)
        }
    }

    fun trans(origin: String): String = getProperty(origin.asKey) ?: let {
        put(origin.asKey, origin)
        save()
        origin
    }

    companion object {
        val dir = Config.dataDirectory.resolve("lang")
        val header = """
                |# Auto generated(自动生成的文件)
                |# backup before modify(修改前注意备份)
                |
            """.trimMargin()
        private val String.asKey get() = "HASH" + hashCode().toString()
    }
}

val cache = mutableMapOf<String, Lang>()
fun getLang(lang: String) = cache.getOrPut(lang) { Lang(lang) }

registerVar(TemplateHandlerKey, "多语言处理", TemplateHandler.new {
    when (val p = getVar("receiver")) {
        null -> getLang(console).trans(it)//console
        is Player -> getLang(p.lang).trans(it)
        else -> it
    }
})

//===commands===
val commands = Commands()
commands += CommandInfo(this, "reload", "重载语言文件") {
    permission = "wayzer.lang.reload"
    body {
        cache.clear()
        reply("[green]缓存已刷新".with())
    }
}
commands += CommandInfo(this, "setDefault", "设置玩家默认语言") {
    permission = "wayzer.lang.setDefault"
    body {
        arg.getOrNull(0)?.let { default = it }
        reply("[green]玩家默认语言已设为 {v}".with("v" to default))
    }
}
commands += CommandInfo(this, "set", "设置当前使用语言") {
    permission = "wayzer.lang.setDefault"
    body {
        val suffix = if (player == null) ".console" else ".user"
        if (arg.isEmpty())
            returnReply("[yellow]可用语言: {list}".with(
                "list" to Lang.dir.listFiles { it -> it.nameWithoutExtension.endsWith(suffix) }.orEmpty().map {
                    it.nameWithoutExtension.removeSuffix(suffix)
                }
            ))
        else {
            if (player == null) {//console
                console = arg[0]
                returnReply("[green]控制台语言已设为 {v}".with("v" to console))
            } else {
                player!!.lang = arg[0]
                returnReply("[green]你的语言已设为 {v}".with("v" to player!!.lang))
            }
        }
    }
}
command("lang", "设置语言") {
    body(commands)
    onComplete(commands)
}
