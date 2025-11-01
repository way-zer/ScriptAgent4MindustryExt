@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("com.github.way-zer:ContentsTweaker:v3.1.2", mavenDependsSingle = true)

package coreMindustry

import arc.struct.Seq
import arc.util.serialization.Jval

registerVar("scoreboard.ext.contents-0-Version", "ContentPatcher状态显示", DynamicVar {
    if (patches == null) return@DynamicVar null
    "{cK}CP修改已加载: {cV}{count} 修改".with("count" to state.patcher.patches.size)
})

var patches: String?
    get() = state.map.tags.get("ContentsPatch")
    set(v) {
        state.map.tags.put("ContentsPatch", v)
        //back compatibility
        state.rules.tags.put("ContentsPatch", v!!)
    }
var patchList: List<String>
    get() = patches?.split(";").orEmpty()
    set(v) {
        patches = v.joinToString(";")
    }

@JvmName("addPatchV3")
fun addPatch(name: String, patch: String) {
    //logger.info("Adding patch $name")
    if (!name.startsWith("$")) {
        state.map.tags.put("CT@$name", patch)
        patchList = patchList.toMutableList().apply {
            remove(name); add(name)//put last
        }
    }

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

    val readPatch = Jval.read(raw).toString(Jval.Jformat.plain)
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
