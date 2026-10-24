package coreMindustry.lib

import arc.Core
import arc.util.Interval
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.MainEventLoop
import kotlinx.coroutines.*
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

object MindustryDispatcher : CoroutineDispatcher() {
    private val warnTimer = Interval()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return MainEventLoop.isDispatchNeeded(context)
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        return MainEventLoop.post(block)
    }

    @OptIn(InternalCoroutinesApi::class)
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        if (warnTimer[10 * 60f])
            thisContextScript().logger.log(
                Level.WARNING, "avoid use yield() in Dispatchers.game, use nextTick instead", Exception()
            )
        MainEventLoop.post(block)
    }

    /**
     * run in mindustry main thread
     * call [MainEventLoop.post] when need
     * @see [runInMain] with catch to prevent close main thread
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun runInMainUnsafe(block: Runnable) {
        if (!isDispatchNeeded(EmptyCoroutineContext)) block.run()
        else MainEventLoop.post(block)
    }

    /**
     * run in mindustry main thread
     * call [MainEventLoop.post] when need
     */
    fun runInMain(block: Runnable) {
        runInMainUnsafe {
            try {
                block.run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    @Deprecated("not pass CoroutineScope", ReplaceWith("safeBlocking(block)"), DeprecationLevel.HIDDEN)
    fun <T> safeBlocking(block: suspend CoroutineScope.() -> T): T {
        return safeBlocking {
            coroutineScope {
                block().also {
                    cancelChildren(CancellationException("safeBlocking End, not use this coroutineScope"))
                }
            }
        }
    }

    fun <T> safeBlocking(block: suspend () -> T): T = MainEventLoop.runBlocking(block)

    object Post : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            MainEventLoop.post(block)
        }
    }
}

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("return CoroutineDispatcher instead MindustryDispatcher", level = DeprecationLevel.HIDDEN)
val Dispatchers.game
    get() = MindustryDispatcher
@Suppress("unused", "UnusedReceiverParameter")
val Dispatchers.game: CoroutineDispatcher
    @JvmName("gameV2")
    get() = MindustryDispatcher

@Suppress("unused", "UnusedReceiverParameter")
@Deprecated("Don't rely on Dispatcher for nextTick", ReplaceWith("Dispatchers.game"))
val Dispatchers.gamePost
    get() = MindustryDispatcher.Post

//suspend fun nextTick() = yield()
suspend fun nextTick() {
    suspendCancellableCoroutine { cont ->
        Core.app.post { cont.resume(Unit) }
    }
}