@file:Suppress("unused")

package coreLibrary.lib

/**
 * 异步Api
 * 该Api已全部弃用,请使用直接在脚本内使用协程(能够在disable时自动清理)
 * 目前有两种异步接口
 * Timer和协程
 */
import kotlinx.coroutines.*
import java.util.*

/**
 * use script.enabled to cancel to prevent leak
 */
@Deprecated("use Coroutines")
object SharedTimer : Timer("ScriptAgentTimer", true)

/**
 * use script.enabled to cancel to prevent leak
 */
@Deprecated("use script's")
val SharedCoroutineScope = GlobalScope +
        SupervisorJob() + Dispatchers.Default + CoroutineName("ScriptAgentCoroutine")