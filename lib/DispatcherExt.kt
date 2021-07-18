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

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        runInMainUnsafe(block)//Already has catcher in coroutine
    }

    /**
     * run in mindustry main thread
     * call [Core.app.post()] when need
     * @see [runInMain] with catch to prevent close main thread
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun runInMainUnsafe(block: Runnable) {
        if (Thread.currentThread() == mainThread) block.run()
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