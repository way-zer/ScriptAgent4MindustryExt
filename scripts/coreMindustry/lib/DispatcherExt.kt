package coreMindustry.lib

import arc.Core
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

object MindustryDispatcher : CoroutineDispatcher() {
    private var mainThread: Thread? = null

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