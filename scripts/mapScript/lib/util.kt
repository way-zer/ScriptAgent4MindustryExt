package mapScript.lib

import cf.wayzer.placehold.VarString
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptInfo
import coreLibrary.lib.with
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

/** 为onEnable中使用，确保玩家能够收到信息 */
fun Script.delayBroadcast(msg: VarString) = launch(Dispatchers.gamePost) {
    broadcast(msg)
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

fun Script.checkEnabled(script: ScriptInfo): Boolean {
    if (script.enabled) {
        delayBroadcast("[yellow]加载地图脚本完成: {id}".with("id" to script.id))
    } else {
        delayBroadcast(
            "[red]地图脚本{id}加载失败，请联系管理员: {reason}"
                .with("id" to script.id, "reason" to script.failReason.orEmpty())
        )
    }
    return script.enabled
}