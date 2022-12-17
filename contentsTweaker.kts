@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("cf.wayzer:ContentsTweaker:v2.0.2", mavenDependsSingle = true)

package coreMindustry

import arc.util.serialization.Base64Coder
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.placehold.DynamicVar
import mindustry.game.EventType.PlayEvent
import mindustry.io.JsonIO
import java.security.MessageDigest

var patches: String?
    get() = state.rules.tags.get("ContentsPatch")
    set(v) {
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

//patch
@Savable(false)
val patchMap = mutableMapOf<String, String>()//name->text
customLoad(::patchMap, patchMap::putAll)

val md5Digest = MessageDigest.getInstance("md5")!!

/**
 * @return 最终保存的patch name(可能带md5)
 */
fun addPatch(name: String, patch: String): String {
    val md5 = Base64Coder.encode(md5Digest.digest(patch.toByteArray())).concatToString()
    val name2 = if (name.startsWith("$")) name else "$name-$md5"
    patchMap[name2] = patch
    PatchHandler.handle(JsonIO.read(null, patch))
    PatchHandler.doAfterHandle()
    patchList = patchList.toMutableList().apply {
        remove(name2)
        add(name2)//put last
    }
    Call.clientPacketReliable("ContentsLoader|loadPatch", md5)
    return name2
}
export(::addPatch)
listen<PlayEvent> {
    patchList.run {
        if (isEmpty()) return@run
        forEach {
            val patch = patchMap[it] ?: return@forEach
            PatchHandler.handle(JsonIO.read(null, patch))
        }
        PatchHandler.doAfterHandle()
    }
}
listen<EventType.ResetEvent> {
    PatchHandler.recoverAll()
}

//处理客户端请求
onEnable {
    netServer.addPacketHandler("ContentsLoader|requestPatch") { p, msg ->
        patchMap[msg]?.let {
            Call.clientPacketReliable(p.con, "ContentsLoader|newPatch", msg + "\n" + it)
        }
    }
}

onDisable {
    netServer.getPacketHandlers("ContentsLoader|requestPatch").clear()
}