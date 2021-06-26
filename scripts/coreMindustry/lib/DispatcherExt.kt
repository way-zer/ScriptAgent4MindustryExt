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
        runInMain(block)
    }

    /**
     * run in mindustry main thread
     * call [Core.app.post()] when need
     */
    fun runInMain(block: Runnable) {
        if (Thread.currentThread() == mainThread) block.run()
        else Core.app.post(block)
    }
}

@Suppress("unused")
val Dispatchers.game
    get() = MindustryDispatcher