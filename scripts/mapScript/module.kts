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

onEnable {
    //Disable all non-controller scripts
    children.forEach {
        if (it.scriptState == ScriptState.ToEnable && it.inst?.mapScriptController != true) {
            it.stateUpdateForce(ScriptState.Loaded)
        }
    }
}

listen<EventType.ResetEvent> {
    MindustryDispatcher.safeBlocking {
        ScriptManager.transaction {
            addAll(children)
            disable()
            getForState(ScriptState.ToEnable).forEach {
                it.stateUpdateForce(ScriptState.Loaded)
            }
        }
    }
}

listen<EventType.ResetEvent> {
    //try update child scripts
    ScriptRegistry.scanRoot()
    MindustryDispatcher.safeBlocking {
        ScriptManager.transaction {
            addAll(children)
            removeIf { it.compiledScript?.source.run { this == null || this == it.source } }
            if (isEmpty()) return@transaction

            logger.info("Unload outdated script: ${toList()}")
            unload()//unload all updatable
        }
    }
}

fun getToLoadMapScripts(): List<ScriptInfo> {
    return buildList {
        ScriptManager.getScriptNullable("mapScript/${MapManager.current.id}")?.id?.let { add(it) }
        state.rules.tags.get("@mapScript")?.let { add("mapScript/${it.toIntOrNull() ?: MapManager.current.id}") }
        addAll(TagSupport.findTags(state.rules).values)
    }.mapNotNull { scriptId ->
        ScriptRegistry.getScriptInfo(scriptId) ?: null.also {
            delayBroadcast("[red]该服务器不存在对应地图脚本，请联系管理员: {id}".with("id" to scriptId))
        }
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
        ScriptManager.transaction {
            addAll(toLoad)
            load(); enable()
        }
    }
    toLoad.forEach { checkEnabled(it) }
}

onEnable {
    MapRegistry.register(this, ScriptMapGenerator.Provider)
}