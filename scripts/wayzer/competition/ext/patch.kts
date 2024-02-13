@file:Depends("wayzer/maps", "获取地图信息")
@file:Depends("coreMindustry/menu", "菜单选人")
package wayzer.competition.ext

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi.with
import coreLibrary.lib.Commands.Companion.helpInfo
import coreLibrary.lib.registerVarForType
import coreMindustry.PagedMenuBuilder
import coreMindustry.lib.MsgType
import coreMindustry.lib.listen
import coreMindustry.lib.sendMessage
import mindustry.Vars
import mindustry.game.EventType
import wayzer.MapInfo

var patchesToLoad : Collection<MapPatch> = setOf()
export(::patchesToLoad)

@Savable
var loadedPatches : Collection<MapPatch> = setOf()

registerVarForType<MapPatch>().apply {
    registerChild("name", "突变名", DynamicVar.obj { it.name })
    registerChild("desc", "突变描述", DynamicVar.obj { it.desc })
    registerChild("env", "适用环境", DynamicVar.obj { it.env.joinToString { it.name } })
}
registerVar("scoreBroad.ext.mapPatch", "地图突变", DynamicVar.v {
    "[violet]本地图带有以下突变：${loadedPatches.joinToString { it.name }}".takeIf { loadedPatches.isNotEmpty() }
})

BuiltinPatch.onLoad(thisContextScript())

listen<EventType.WorldLoadBeginEvent> {
    if (patchesToLoad.isEmpty()) return@listen
    patchesToLoad.forEach{it()}
    loadedPatches = patchesToLoad
    patchesToLoad = setOf()
}

listen<EventType.PlayerJoin> {
    if (loadedPatches.isEmpty()) return@listen
    with(it.player) {
        var msg = ("" +
            "| [violet]本局游戏地图带有以下突变[]" +
        "").trimMargin()
        for (patch in loadedPatches) {
            msg += """
                | [accent][gold]{mapPatch.name}[]
                | [gold]{mapPatch.desc}[]
            """.trimMargin().with("mapPatch" to patch)
        }
        sendMessage(msg.with(), MsgType.InfoMessage)
    }
}

command("patch", "地图突变指令") {
    body(commands)
}
val commands = Commands()
commands += CommandInfo(this, "list", "突变列表") {
    body {
        if (player != null) {
            PagedMenuBuilder(PatchManager.patches, selectedPage = arg.firstOrNull()?.toIntOrNull() ?: 1) { patch ->
                option(buildString {
                    append("[gold]${patch.name}")
                    appendLine(" [white]${patch.desc}")
                    append("[cyan]适用环境：${patch.env.joinToString { it.name }}")
                }) {
                    close()
                }
            }.sendTo(player!!, 60_000)
        } else {
            val page = arg.firstOrNull()?.toIntOrNull() ?: 1
            reply(menu("地图突变列表", PatchManager.patches, page, 10) {
                "[light_yellow]{name} [white]{desc}  [light_cyan]{env}".with(
                        "name" to it.name, "desc" to it.desc,
                        "env" to it.env.joinToString { env -> env.name }
                )
            })
        }
    }
}