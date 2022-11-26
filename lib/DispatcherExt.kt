package coreMindustry.lib

import arc.Core
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext

object MindustryDispatcher : CoroutineDispatcher() {
    private var mainThread: Thread? = null
    private var blockingQueue = ConcurrentLinkedQueue<Runnable>()

    @Volatile
    private var inBlocking = false

    init {
        Core.app.post {
            mainThread = Thread.currentThread()
        }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        if (Thread.currentThread() == mainThread || mainThread?.isAlive != true)
            return false
        return true
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

    fun <T> safeBlocking(block: suspend CoroutineScope.() -> T): T {
        return if (inBlocking) runBlocking(Dispatchers.game, block)
        else runBlocking {
            inBlocking = true
            launch {
                while (inBlocking || blockingQueue.isNotEmpty()) {
                    blockingQueue.poll()?.run() ?: yield()
                }
            }
            withContext(Dispatchers.game, block).also {
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