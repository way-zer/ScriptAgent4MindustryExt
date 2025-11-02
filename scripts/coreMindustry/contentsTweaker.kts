@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Depends("coreMindustry/menu", "调用菜单")

package coreMindustry

import arc.struct.Seq
import arc.util.serialization.Jval
import mindustry.mod.ContentPatcher.PatchSet

@JvmName("addPatchV3")
fun addPatch(name: String, patch: String = "PatchFromContentsTweaker") {
    val raw = patch
        .replace("+=", "+")
        .replace("#", "arg")
        .replace(Regex("""(:)([\u4e00-\u9fa5][^,\}\]]*)""")) { m ->
            val sep = m.groupValues[1]
            val text = m.groupValues[2].trim()
            "$sep\"$text\""
        }
        .replace(Regex("(?<=\\{|,|\\s)([a-zA-Z0-9_-]+):"), "\"$1\":")
        .replace(Regex(":\\s*([a-zA-Z_-]+)(?=\\s*[},])")) { m ->
            ":\"${m.groupValues[1]}\""
        }

    val readPatch = Jval.read(raw).apply { asObject(); put("name", Jval.valueOf(name)) }.toString(Jval.Jformat.plain)
    logger.info(readPatch)
    state.patcher.apply(state.patcher.patches.map { it.patch }.add(readPatch))
}
@JvmName("addPatch")
fun addPatchOld(name: String, patch: String): String {
    addPatch(name, patch)
    return name
}
export(::addPatch)
listen<EventType.ResetEvent> {
    state.patcher.apply(Seq())//may load server global patches
}

listen<EventType.WorldLoadBeginEvent> {
    state.map.tags.get("ContentsPatch")?.split(";")?.forEach { name ->
        if (name.isBlank()) return@forEach
        val patch = state.map.tags.get("CT@$name") ?: return@forEach
        addPatch(name, patch)
    }
}

command("cp", "查看cp修改") {
    type = CommandType.Client
    body {
        menu(player!!)
    }
}

suspend fun menu(player: Player, select: Int = 1) {
    object : PagedMenuBuilder<PatchSet>(state.patcher.patches.toList(), selectedPage = select, 6) {
        override suspend fun renderItem(item: PatchSet) {
            option(if (item.name.isEmpty()) "<unnamed>" else item.name) {
                moreMenu(player, selectedPage, item)
            }
        }

        override suspend fun build() {
            title = "[yellow]ContentPatcher"
            msg = """
            [sky]查询服务器patch列表
            """.trimIndent()
            super.build()
        }
    }.sendTo(player, 20_000)
}

suspend fun moreMenu(player: Player, select: Int, item: PatchSet) {
    MenuBuilder(true){
        title = "[yellow]Patch详情"
        val jsonFlat = try {
            val jval = Jval.read(item.patch)
            flater(jval)
        } catch (e: Exception) {
            "[red]解析失败"
        }
        msg = """
            [white]${jsonFlat}
        """.trimIndent()
        option ("[red]返回"){
            menu(player, select)
        }
    }.sendTo(player, 20_000)
}

fun flater(jval: Jval, prefix: String = ""): String = buildString {
    when {
        jval.isObject -> jval.asObject().forEach {
            val fullKey = if (prefix.isEmpty()) it.key else "$prefix.${it.key}"
            append(flater(it.value, fullKey))
        }
        jval.isArray -> jval.asArray().forEachIndexed { index, value ->
            append(flater(value, "$prefix[$index]"))
        }
        else -> append("$prefix = $jval\n")
    }
}
