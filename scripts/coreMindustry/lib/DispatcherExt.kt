package coreMindustry.lib

import arc.Core
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

object MindustryDispatcher : CoroutineDispatcher() {
    private var mainThread: Thread? = null

    init {
        Core.app.post {
            mainThread = Thread.currentThread()
        }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() != mainThread && mainThread?.isAlive == true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Core.app.post(block)//Already has catcher in coroutine
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
}

@Suppress("unused")
val Dispatchers.game
    get() = MindustryDispatcher