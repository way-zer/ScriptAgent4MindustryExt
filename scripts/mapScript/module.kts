@file:Depends("coreMindustry")
@file:Depends("wayzer/maps", "获取地图信息")
@file:Depends("wayzer/map/mapInfo", "显示地图信息", soft = true)
@file:Import("mapScript.lib.*", defaultImport = true)

package mapScript

import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import cf.wayzer.scriptAgent.events.ScriptStateChangeEvent
import wayzer.MapInfo
import wayzer.MapManager
import wayzer.MapProvider
import wayzer.MapRegistry
import java.util.logging.Level
import kotlin.system.measureTimeMillis

val moduleId = id

@Savable(serializable = false)
@Volatile
var inRunning: ScriptInfo? = null

listen<EventType.ResetEvent> {
    runBlocking {
        ScriptManager.transaction {
            add("$moduleId/")
            disable()
            getForState(ScriptState.ToEnable).forEach {
                it.stateUpdateForce(ScriptState.Loaded)
            }
        }
    }
    inRunning = null
}

fun loadMapScript(id: String): Boolean {
    val script = ScriptManager.getScriptNullable(id)?.scriptInfo
    if (script == null) {
        launch(Dispatchers.gamePost) {
            broadcast("[red]该服务器不存在对应地图脚本，请联系管理员: {id}".with("id" to id))
        }
        return false
    }
    inRunning = script
    if (script.enabled) return true
    runBlocking {
        ScriptManager.enableScript(script, true)
    }
    launch(Dispatchers.gamePost) {
        if (script.enabled)
            broadcast("[yellow]加载地图特定脚本完成: {id}".with("id" to script.id))
        else
            broadcast(
                "[red]地图脚本{id}加载失败，请联系管理员: {reason}"
                    .with("id" to script.id, "reason" to script.failReason.orEmpty())
            )
    }
    return script.enabled
}

listen<EventType.PlayEvent> {
    val scriptId = ScriptManager.getScriptNullable("$moduleId/${MapManager.current.id}")?.id
        ?: state.rules.tags.get("@mapScript")
            ?.run { "$moduleId/${toIntOrNull() ?: MapManager.current.id}" }
        ?: return@listen
    loadMapScript(scriptId)
}

//阻止其他脚本启用
listenTo<ScriptStateChangeEvent.Cancellable>(Event.Priority.Intercept) {
    fun checkId() = script.id.startsWith(moduleId)
            && script.id != inRunning?.id && inRunning?.dependsOn(script.scriptInfo, includeSoft = true) != true
    when (next) {
        ScriptState.ToEnable -> if (checkId()) cancelled = true
        ScriptState.Enabling -> if (checkId()) {
            cancelled = true
            script.stateUpdateForce(ScriptState.Loaded).join()
        }

        else -> {}
    }
}

//检测Generator脚本
listenTo<ScriptStateChangeEvent>(Event.Priority.Watch) {
    if (next == ScriptState.Loaded && script.id.startsWith(moduleId)) {
        script.inst?.let(GeneratorSupport::checkScript)
    }
}

//loadGenerator
listenTo<ScriptEnableEvent>(Event.Priority.Before) {
    val map = script.mapInfo ?: return@listenTo
    try {
        world.loadGenerator(map.width, map.height) { tiles ->
            script.genRound.forEach { (name, round) ->
                val time = measureTimeMillis { round(tiles) }
                script.logger.info("Do $name costs $time ms.")
            }
        }
    } catch (e: Throwable) {
        script.logger.log(Level.SEVERE, "loadGenerator出错", e)
        ScriptManager.disableScript(script, "loadGenerator出错: $e")
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