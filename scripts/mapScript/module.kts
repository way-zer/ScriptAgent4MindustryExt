@file:Depends("coreMindustry")
@file:Depends("wayzer/maps", "获取地图信息")
@file:Depends("wayzer/map/mapInfo", "显示地图信息", soft = true)
@file:Import("mapScript.lib.*", defaultImport = true)

/**
 * 该模块定义了一种特殊的kts：kts的生命周期与地图关联。
 * 当地图满足特定条件时(id/tag)，关联的kts会被enable，而一局游戏结束后，所有的kts会被disable。
 * */
package mapScript

import cf.wayzer.scriptAgent.events.ScriptStateChangeEvent
import wayzer.MapManager

val moduleId = id

val toEnable = mutableSetOf<ScriptInfo>()

listen<EventType.ResetEvent> {
    toEnable.clear()
    MindustryDispatcher.safeBlocking {
        ScriptManager.transaction {
            add("$moduleId/")
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
    ScriptManager.transaction {
        add("$moduleId/")
        removeIf { it.compiledScript?.source.run { this == null || this == it.source } }
        if (isNotEmpty()) MindustryDispatcher.safeBlocking {
            logger.info("Unload outdated script: ${toList()}")
            unload()//unload all updatable
        }
    }
}

listen<EventType.PlayEvent> {
    //load scripts
    val scriptId = ScriptManager.getScriptNullable("$moduleId/${MapManager.current.id}")?.id
        ?: state.rules.tags.get("@mapScript")
            ?.run { "$moduleId/${toIntOrNull() ?: MapManager.current.id}" }
    if (scriptId != null)
        loadMapScript(scriptId)
    TagSupport.findTags(state.rules).values.toSet().forEach(::loadMapScript)
}

//阻止其他脚本启用
listenTo<ScriptStateChangeEvent.Cancellable>(Event.Priority.Intercept) {
    if (!script.id.startsWith("$moduleId/")) return@listenTo
    fun allowEnable() = toEnable.any { it.dependsOn(script.scriptInfo, includeSoft = true) }
    when (next) {
        ScriptState.ToEnable -> if (!allowEnable()) cancelled = true
        ScriptState.Enabling -> if (!allowEnable()) {
            cancelled = true
            script.stateUpdateForce(ScriptState.Loaded).join()
        }

        else -> {}
    }
}

GeneratorSupport//init
command("mapScriptLoad", "测试: 加载指定地图脚本") {
    permission = "$dotId.load"
    usage = "<script>"
    body {
        val script = arg.firstOrNull() ?: replyUsage()
        loadMapScript(script)
    }
}