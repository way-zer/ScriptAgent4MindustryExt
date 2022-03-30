@file:Depends("coreMindustry")
@file:Depends("wayzer/maps", "获取地图信息")

package mapScript

import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import wayzer.MapManager

val moduleId = id

@Savable(serializable = false)
@Volatile
var inRunning: ScriptInfo? = null

listen<EventType.ResetEvent> {
    inRunning?.let { ScriptManager.disableScript(it, "按需关闭") }
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
    launch {
        ScriptManager.loadScript(script, true)
        broadcast("[yellow]加载地图特定脚本完成: {id}".with("id" to script.id))
    }
}

listenTo<ScriptEnableEvent>(Event.Priority.Intercept) {
    if (script.id.startsWith("$moduleId/") && script.id != inRunning?.id)
        ScriptManager.disableScript(script, "按需关闭")
}