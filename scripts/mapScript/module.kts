@file:Depends("coreMindustry")
@file:Depends("wayzer/maps", "获取地图信息")
@file:Depends("wayzer/map/mapInfo", "显示地图信息", soft = true)
@file:Import("mapScript.lib.*", defaultImport = true)

/**
 * 该模块定义了一种特殊的kts：kts的生命周期与地图关联。
 * 当地图满足特定条件时(id/tag)，关联的kts会被enable，而一局游戏结束后，所有的kts会被disable。
 * */
package mapScript

import wayzer.MapManager
import wayzer.MapRegistry

val children get() = ScriptRegistry.allScripts { it != scriptInfo && it.dependsOn(scriptInfo) }

listen<EventType.ResetEvent> { _ ->
    MindustryDispatcher.safeBlocking {
        ScriptManager.transactionV2 {
            disable(children.filter { it.enabled })
            execute().printResult()
            load(keys.toList())
        }.printResult()
    }
}

fun getToLoadMapScripts(): List<ScriptInfo> {
    //匹配所有mapScript子脚本，且名字与id匹配的
    val children = children
    val byId = children.find { it.id.endsWith("/${MapManager.current.id}") }
    val byTag = state.rules.tags.get("@mapScript")?.let { tag ->
        val tagId = tag.toIntOrNull() ?: MapManager.current.id
        children.find { it.id.endsWith("/$tagId") } ?: null.also {
            delayBroadcast("[red]该服务器不存在对应地图脚本，请联系管理员: {id}".with("id" to tagId))
        }
    }
    return buildList {
        if (byId != null) add(byId)
        if (byTag != null && byTag != byId) add(byTag)
        addAll(TagSupport.findTags(state.rules).values)
    }.flatMap {
        listOf(it) + ScriptRegistry.allScripts { dep ->
            !dep.enabled && it.dependsOn(dep, includeSoft = true)
        }
    }.toSet().toList()
}

listen<EventType.ContentPatchLoadEvent> { e ->
    val patches = getToLoadMapScripts().flatMap { it.inst?.mapPatches.orEmpty() }
    if (patches.isEmpty()) return@listen
    logger.info("Patches loaded: ${patches.size}")
    e.patches.addAll(patches)
}

listen<EventType.WorldLoadEvent> {
    //load scripts
    val toLoad = getToLoadMapScripts()
    if (toLoad.isEmpty()) return@listen
    MindustryDispatcher.safeBlocking {
        ScriptManager.transactionV2 {
            enable(toLoad)
        }
    }
    toLoad.forEach { checkEnabled(it) }
}

onEnable {
    MapRegistry.register(this, ScriptMapGenerator.Provider)
}