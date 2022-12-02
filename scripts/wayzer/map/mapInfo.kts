package wayzer.map

import cf.wayzer.placehold.DynamicVar
import mindustry.gen.Groups

@Savable(false)
val customModeIntroduce = mutableListOf<String>()
customLoad(::customModeIntroduce, customModeIntroduce::addAll)

fun addModeIntroduce(mode: String, introduce: String) {
    if (customModeIntroduce.any { it.contains("===[gold]$mode[]===") }) return
    customModeIntroduce += "[magenta]===[gold]$mode[]===[white]\n$introduce"
}
export(this::addModeIntroduce)
listen<EventType.ResetEvent> { customModeIntroduce.clear() }

registerVar("scoreBroad.ext.customMode", "自定义模式Tip", DynamicVar.v {
    "[violet]本地图有自定义模式,详情使用[orange]/mapInfo[]查看".takeIf { customModeIntroduce.isNotEmpty() }
})


fun Player.showDetail() {
    val pages = (listOf(buildString {
        appendLine("[white]${state.map.name()}")
        appendLine()
        appendLine("[purple]By: [scarlet]${state.map.author()}")
        appendLine("[white]${state.map.description()}")
    }) + customModeIntroduce).autoPage()
    for (page in pages.size downTo 1) {
        sendMessage(
            "[green]==== [white]地图信息[] ====\n{body}\n[green]==== [white]{page}/{total}[] ===="
                .with("body" to pages[page - 1], "page" to page, "total" to pages.size), type = MsgType.InfoMessage
        )
    }
}

fun Player.showInfo() {
    if (con == null) return
    val desc = state.map.description().autoWrapLine()
    val msg = buildString {
        appendLine("[white]${state.map.name()}")
        appendLine()
        appendLine("[purple]By: [scarlet]${state.map.author()}")
        appendLine("[white]$desc")
        if (customModeIntroduce.isNotEmpty()) {
            appendLine()
            appendLine("[yellow]本地图共有${customModeIntroduce.size}个特殊模式")
            appendLine("具体请使用[gold]/mapinfo[]查看")
        }
    }
    Call.label(con, msg, 2 * 60f, core()?.x ?: 0f, core()?.y ?: 0f)
}

listen<EventType.PlayEvent> {
    launch(Dispatchers.gamePost) {
        Groups.player.forEach {
            it.showInfo()
        }
    }
}

listen<EventType.PlayerJoin> { e ->
    Core.app.post { e.player.showInfo() }
}

command("mapInfo", "地图详情") {
    type = CommandType.Client
    body {
        player!!.showDetail()
    }
}

//region util
fun Iterable<String>.autoPage(lines: Int = 10): List<String> {
    val out = mutableListOf<String>()
    var acc = ""
    fun add() {
        if (acc.isBlank()) return
        out.add(acc)
        acc = ""
    }
    for (text in this) {
        acc += text
        if (acc.lineSequence().count() > lines) add()
    }
    add()
    return out
}

fun String.autoWrapLine(limit: Int = 25): String {
    var lastChar = ' '
    var i = 0
    return map {
        if (i > limit && it in charArrayOf(' ', '，', ',', '.', '。', '!', '！'))
            if (it != '.' || lastChar.code !in '0'.code..'9'.code) {
                i = 0
                lastChar = it
                return@map '\n'
            }
        lastChar = it
        i++
        return@map it
    }.joinToString("")
}
//endregion