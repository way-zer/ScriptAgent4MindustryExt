@file:Depends("coreMindustry")
@file:Depends("wayzer/maps", "获取地图信息")
@file:Import("mapScript.lib.*", defaultImport = true)

package mapScript

import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import wayzer.MapInfo
import wayzer.MapManager
import wayzer.MapProvider
import wayzer.MapRegistry

val moduleId = id

@Savable(serializable = false)
@Volatile
var inRunning: ScriptInfo? = null

listen<EventType.ResetEvent> {
    ScriptManager.allScripts { it.id.startsWith("$moduleId/") && it.enabled }
        .forEach { ScriptManager.disableScript(it, "按需关闭") }
    inRunning = null
}

listen<EventType.PlayEvent> {
    fun getScriptByTag(): ScriptInfo? {
        val scriptId = state.rules.tags.get("@mapScript")?.run { toIntOrNull() ?: MapManager.current.id } ?: return null
        val script = ScriptManager.getScriptNullable("$moduleId/$scriptId")?.scriptInfo
        if (script == null)
            broadcast("[red]该服务器不存在对应地图脚本，请联系管理员: {id}".with("id" to scriptId))
        return script
    }

    val script = ScriptManager.getScriptNullable("$moduleId/${MapManager.current.id}")?.scriptInfo
        ?: getScriptByTag() ?: return@listen
    inRunning = script
    if (script.enabled) return@listen
    ScriptManager.newLoadScript(script, true)
    launch(Dispatchers.gamePost) {
        broadcast("[yellow]加载地图特定脚本完成: {id}".with("id" to script.id))
    }
}

@OptIn(LoaderApi::class)
listenTo<ScriptEnableEvent>(Event.Priority.Intercept) {
    if (script.id.startsWith("$moduleId/")) {
        GeneratorSupport.checkScript(script)
        if (script.id != inRunning?.id && inRunning?.dependsOn(script.scriptInfo, includeSoft = true) != true)
            ScriptManager.disableScript(script, "按需关闭")
    }
}

MapRegistry.register(this, object : MapProvider() {
    override val supportFilter: Set<String> get() = GeneratorSupport.knownMaps.flatMapTo(mutableSetOf()) { it.value.second }
    override fun getMaps(filter: String) = GeneratorSupport.knownMaps.values
        .filter { filter in it.second }
        .map { it.first }

    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        return GeneratorSupport.findGenerator(id)
    }
})