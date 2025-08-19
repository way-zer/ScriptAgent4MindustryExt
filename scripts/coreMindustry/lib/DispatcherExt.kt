package coreMindustry.lib

import arc.Core
import arc.util.Interval
import cf.wayzer.scriptAgent.thisContextScript
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import kotlin.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

object MindustryDispatcher : CoroutineDispatcher() {
    private var mainThread: Thread? = null
    private var blockingQueue = ConcurrentLinkedQueue<Runnable>()

    @Volatile
    private var inBlocking = false
    private val warnTimer = Interval()

    init {
        Core.app.post {
            mainThread = Thread.currentThread()
        }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() != mainThread && mainThread?.isAlive == true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (inBlocking) {
            blockingQueue.add(block)
            return
        }
        Core.app.post(block)//Already has catcher in coroutine
    }

    @OptIn(InternalCoroutinesApi::class)
    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        if (warnTimer[10 * 60f])
            thisContextScript().logger.log(
                Level.WARNING, "avoid use yield() in Dispatchers.game, use nextTick instead", Exception()
            )
        Core.app.post(block)
    }

    /**
     * run in mindustry main thread
     * call [Core.app.post()] when need
     * @see [runInMain] with catch to prevent close main thread
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun runInMainUnsafe(block: Runnable) {
        if (Thread.currentThread() == mainThread || mainThread?.isAlive == false) block.run()
        else Core.app.post(block)
    }

    /**
     * run in mindustry main thread
     * call [Core.app.post()] when need
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

    fun <T> safeBlocking(block: suspend () -> T): T {
        check(Thread.currentThread() == mainThread) { "safeBlocking only for mainThread" }
        //Should not pass CoroutineScope to this function
        if (inBlocking) return runBlocking(Dispatchers.game) { block() }
        inBlocking = true
        return runBlocking {
            launch {
                while (inBlocking || blockingQueue.isNotEmpty()) {
                    blockingQueue.poll()?.run() ?: yield()
                }
            }
            try {
                withContext(Dispatchers.game) { block() }
            } finally {
                inBlocking = false
            }
        }
    }

    object Post : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (mainThread?.isAlive == false)
                block.run()
            else
                Core.app.post(block)
        }
    }
}

@Suppress("unused")
val Dispatchers.game
    get() = MindustryDispatcher

@Suppress("unused")
val Dispatchers.gamePost
    get() = MindustryDispatcher.Post

//suspend fun nextTick() = yield()
suspend fun nextTick() {
    val context = coroutineContext
    context.ensureActive()
    if (context[ContinuationInterceptor] !is MindustryDispatcher) {
        suspendCoroutine { Core.app.post { it.resume(Unit) } }
        return
    }
    suspendCoroutineUninterceptedOrReturn<Unit> sc@{ cont ->
        val co = cont.intercepted() as? Runnable ?: return@sc Unit
        Core.app.post(co)
        COROUTINE_SUSPENDED
    }
}