package mapScript.lib

import cf.wayzer.scriptAgent.ScriptManager
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptInfo
import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.with
import coreMindustry.lib.MindustryDispatcher
import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import coreMindustry.lib.gamePost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mindustry.Vars
import mindustry.core.GameState
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

//find and ensure loaded
internal fun findAndLoadScript(id: String): ScriptInfo? {
    val script = ScriptRegistry.findScriptInfo(id) ?: return null
    MindustryDispatcher.safeBlocking {
        ScriptManager.transaction {
            add(script)
            load()
        }
    }
    return script.takeIf { it.inst != null }
}

/** 为onEnable中使用，确保玩家能够收到信息 */
fun Script.delayBroadcast(msg: PlaceHoldString) = launch(Dispatchers.gamePost) {
    broadcast(msg)
}

/** 为onEnable中使用，加载其他MapScript脚本 */
fun Script.loadMapScript(id: String, reply: (PlaceHoldString) -> Unit = { delayBroadcast(it) }): Boolean {
    val script = findAndLoadScript(id)?.scriptInfo
    if (script == null) {
        reply("[red]该服务器不存在对应地图脚本，请联系管理员: {id}".with("id" to id))
        return false
    }
    contextScript<mapScript.Module>().toEnable.add(script.scriptInfo)
    if (script.enabled) {
        return true
    }
    MindustryDispatcher.safeBlocking {
        ScriptManager.enableScript(script, true)
    }
    if (script.enabled)
        reply("[yellow]加载地图特定脚本完成: {id}".with("id" to script.id))
    else
        reply(
            "[red]地图脚本{id}加载失败，请联系管理员: {reason}"
                .with("id" to script.id, "reason" to script.failReason.orEmpty())
        )
    return script.enabled
}

@Suppress("UnusedReceiverParameter")
val GameState.gameTime get() = (Vars.state.tick / 60).seconds

/** 延时，直到特定游戏时间(支持暂停) @see [schedule] */
suspend fun delayUntil(gameTime: Duration) {
    while (true) {
        val left = gameTime - Vars.state.gameTime
        if (left.isNegative()) break
        delay(left)
    }
}

/** 计划在特定游戏时间执行 @see [delayUntil] */
fun CoroutineScope.schedule(
    time: Duration,
    context: CoroutineContext = EmptyCoroutineContext,
    body: suspend CoroutineScope.() -> Unit
) {
    if (Vars.state.gameTime > time) return
    launch(Dispatchers.game + context) {
        delayUntil(time)
        body()
    }
}