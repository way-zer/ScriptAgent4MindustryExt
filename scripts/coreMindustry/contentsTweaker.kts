@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("com.github.way-zer:ContentsTweaker:v3.1.2", mavenDependsSingle = true)

package coreMindustry

import arc.Events
import arc.struct.Seq
import arc.util.serialization.Jval
import mindustry.game.EventType.ContentPatchLoadEvent


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

val contentPatches = Seq<String>() //cp in maps may not load here

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
    contentPatches.add(readPatch)
    Events.fire(ContentPatchLoadEvent(contentPatches))
    state.patcher.apply(contentPatches)
}
@JvmName("addPatch")
fun addPatchOld(name: String, patch: String): String {
    addPatch(name, patch)
    return name
}
export(::addPatch)
listen<EventType.ResetEvent> {
    //logger.info("reset")
    contentPatches.clear()
    Events.fire(ContentPatchLoadEvent(contentPatches))//actually empty
    state.patcher.apply(contentPatches)//actually empty
}

listen<EventType.WorldLoadBeginEvent> {
    state.map.tags.get("ContentsPatch")?.split(";")?.forEach { name ->
        if (name.isBlank()) return@forEach
        val patch = state.map.tags.get("CT@$name") ?: return@forEach
        addPatch(name, patch)
    }
}

listen<ContentPatchLoadEvent> {
    //logger.info("loading patch")
    for (patch in contentPatches) {
        it.patches.addUnique(patch)
    }
}
