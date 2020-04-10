@file:Suppress("unused")

package coreLibrary.lib

/**
 * 异步Api
 * 目前有两种异步接口
 * Timer和协程
 */
import kotlinx.coroutines.*
import java.util.*

/**
 * use script.enabled to cancel to prevent leak
 */
object SharedTimer : Timer("ScriptAgentTimer", true)

/**
 * use script.enabled to cancel to prevent leak
 */
val SharedCoroutineScope = GlobalScope +
        SupervisorJob() + Dispatchers.Default + CoroutineName("ScriptAgentCoroutine")