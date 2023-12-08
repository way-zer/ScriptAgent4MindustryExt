@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("cf.wayzer:ContentsTweaker:v3.0.1", mavenDependsSingle = true)

package coreMindustry

import cf.wayzer.contentsTweaker.ContentsTweaker
import cf.wayzer.placehold.DynamicVar

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


registerVar("scoreBroad.ext.contentsVersion", "ContentsTweaker状态显示", DynamicVar.v {
    "[violet]特殊修改已加载: [orange](使用[sky]ContentsTweaker[]MOD获得最佳体验)".takeIf { patches != null }
})

fun sendPatch(name: String, patch: String) {
    Call.clientPacketReliable("ContentsLoader|newPatch", "$name\n$patch")
}

@JvmName("addPatchV3")
fun addPatch(name: String, patch: String) {
    if (!name.startsWith("$")) {
        state.map.tags.put("CT@$name", patch)
        patchList = patchList.toMutableList().apply {
            remove(name);add(name)//put last
        }
    }
    ContentsTweaker.loadPatch(name, patch)
    sendPatch(name, patch)
}
@JvmName("addPatch")
fun addPatchOld(name: String, patch: String): String {
    addPatch(name, patch)
    return name
}
export(::addPatch)
listen<EventType.ResetEvent> {
    ContentsTweaker.recoverAll()
}

listen<EventType.WorldLoadBeginEvent> {
    if (ContentsTweaker.worldInReset) return@listen
    var needAfterHandle = false
    state.map.tags.get("ContentsPatch")?.split(";")?.forEach { name ->
        if (name.isBlank()) return@forEach
        val patch = state.map.tags.get("CT@$name") ?: return@forEach
        ContentsTweaker.loadPatch(name, patch, doAfter = false)
        needAfterHandle = true
    }
    if (needAfterHandle) ContentsTweaker.afterHandle()
}

//处理客户端请求
onEnable {
    netServer.addPacketHandler("ContentsLoader|version") { p, msg ->
        logger.info("${p.name} $msg")
        if (msg.contains("2."))
            Call.sendMessage(p.con, "你当前安装的CT版本过老，请更新到3.0.1", null, null)
    }
    netServer.addPacketHandler("ContentsLoader|requestPatch") { p, msg ->
        state.map.tags["CT@$msg"]?.let { sendPatch(msg, it) }
    }
}

onDisable {
    netServer.getPacketHandlers("ContentsLoader|version").clear()
    netServer.getPacketHandlers("ContentsLoader|requestPatch").clear()
}