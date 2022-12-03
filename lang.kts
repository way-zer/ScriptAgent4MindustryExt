package coreLibrary

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.TemplateHandler
import cf.wayzer.placehold.TemplateHandlerKey
import java.io.File
import java.util.*
import kotlin.math.max

name = "国际化多语言"

val fallbackMap by config.key(
    mapOf(
        "overwrite" to "@raw", "*" to "overwrite"
    ), "备用语言映射表,@raw表示内部语言,*代表所有未匹配"
)

var console by config.key("@raw", "控制台语言(不发给玩家的语句)")

fun fallbackLang(lang: String): String {
    return (fallbackMap[lang] ?: fallbackMap["*"] ?: "@raw")
}

data class NewSentenceEvent(val sentence: Sentence) : Event {
    companion object : Event.Handler()
}

inner class Sentence(val raw: String) {
    val translated = mutableMapOf<String, String>()
    var lastUse: Long = -1

    fun get(lang0: String): String {
        lastUse = System.currentTimeMillis()
        var lang: String = lang0
        while (lang != "@raw") {
            translated[lang]?.let { return it }
            lang = fallbackLang(lang)
        }
        return raw
    }
}

private val file: File get() = Config.dataDir.resolve("lang.ini")
val data = mutableMapOf<String, Sentence>()
var lastSave = 0L
fun save() {
    file.bufferedWriter().use { writer ->
        writer.appendLine("# ScriptAgent Lang File")
        writer.appendLine("# field starting with '_' should not modify")
        writer.appendLine("# use '\\n' for newline in value")
        writer.appendLine("# Save in ${Date()}")
        writer.appendLine("# WARNING: this is not completed ini file, some format may not support")
        fun writeKV(key: String, value: String) {
            writer.append(key)
            writer.append("=\"")
            var start = 0
            while (true) {
                val end = value.indexOf('\n', startIndex = start)
                if (end == -1) break
                writer.append(value.subSequence(start, end))
                writer.append("\\n")
                start = end + 1
            }
            writer.append(value.subSequence(start, value.length))
            writer.appendLine("\"")
        }
        data.values.sortedBy { it.raw }.forEach {
            writer.appendLine("[[sentence]]")
            writeKV("_raw", it.raw)
            writer.appendLine("_lastUse=${it.lastUse}")
            it.translated.forEach(::writeKV)
        }
    }
    lastSave = file.lastModified()
}

fun load() {
    if (file.exists().not()) return
    file.bufferedReader().use { reader ->
        val map = mutableMapOf<String, String>()
        fun handle() {
            if (map.isEmpty()) return
            val raw = map.remove("_raw") ?: error("no _raw")
            val lastUse = map.remove("_lastUse")?.toLongOrNull() ?: error("no _lastUse")
            data.getOrPut(raw) { Sentence(raw) }.apply {
                this.lastUse = max(this.lastUse, lastUse)
                translated.putAll(map)
            }
        }
        for (line in reader.lineSequence()) {
            if (line.firstOrNull() == '#') continue
            if (line == "[[sentence]]") {
                handle()
                map.clear()
            } else {
                var start = line.indexOf('=')
                if (start == -1) error("Bad ini line: $line")
                val key = line.substring(0, start)

                start += 1
                val wrapped = line[start] == '"'
                if (wrapped) start++
                val value = StringBuffer(line.length)
                while (true) {
                    val end = line.indexOf('\\', startIndex = start)
                    if (end == -1) break
                    val next = line.getOrNull(end + 1) ?: break
                    value.append(line.subSequence(start, end))
                    start = when (next) {
                        '\\' -> end + 2
                        'n' -> (end + 2).also { value.append('\n') }
                        else -> end + 1
                    }
                }
                value.append(line.subSequence(start, line.length + if (wrapped) -1 else 0))
                map[key] = value.toString()
            }
        }
    }
    lastSave = file.lastModified()
}

registerVarForType<CommandContext.ConsoleReceiver>().registerChild("lang", "控制台语言", DynamicVar.obj { console })

registerVar(TemplateHandlerKey, "多语言处理", TemplateHandler.new {
    val lang = getVarString("receiver.lang") ?: return@new it
    data.getOrPut(it) {
        Sentence(it).also { sentence ->
            launch { NewSentenceEvent(sentence).emitAsync() }
        }
    }.get(lang)
})


val commands = Commands()
commands += CommandInfo(null, "load", "加载语言文件".with()) {
    body {
        load()
        reply("[green]加载成功".with())
    }
}
commands += CommandInfo(null, "save", "手动保存语言到文件".with()) {
    body {
        launch {
            save()
            reply("[green]保存成功".with())
        }
    }
}
commands += CommandInfo(null, "set", "设置控制台使用语言".with()) {
    body {
        if (arg.isEmpty()) returnReply("[yellow]你的当前语言是: {receiver.lang}".with())
        console = arg[0]
        reply("[green]控制台语言已设为 {v}".with("v" to console))
    }
}
onEnable {
    Commands.controlCommand += CommandInfo(this, "lang", "多语言功能".with()) {
        permission = dotId
        body(commands)
    }
    Commands.controlCommand.autoRemove(this)
    launch {
        load()
        while (isActive) {
            delay(60_000)
            if (lastSave < file.lastModified()) {
                logger.warning("自动保存失败: 语言文件已改动，请使用/sa lang save手动保存")
                continue
            }
            save()
        }
    }
}