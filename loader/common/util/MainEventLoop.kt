package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.define.LoaderApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext

/**
 * 将已有的单线程事件循环桥接为可被 Kotlin Coroutines 调度的执行环境，
 * 并解决「在事件循环线程上调用 runBlocking 会死锁」的问题。
 *
 * 线程约束：[runTurn] 与 [runBlocking] 只能在首次调用时绑定的主线程上执行，
 * 且二者不应并发；[post] 可在任意线程调用。
 *
 * 限制：runBlocking 阻塞期间，不经过本桥接层直接投递到底层事件循环的任务
 * 不会被处理，会延迟到 runBlocking 返回后才执行。
 */
object MainEventLoop : CoroutineDispatcher() {
    private val queue = ConcurrentLinkedQueue<Any>()
    private val sentinel = Any()  // 哨兵本身不执行，只作边界标记
    private val awake = Channel<Unit>(Channel.CONFLATED)
    @Volatile
    var mainThread: Thread? = null
        private set
    private var inBlocking = false

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() != mainThread
    }

    override fun dispatch(context: CoroutineContext, block: kotlinx.coroutines.Runnable) {
        post(block)
    }

    private fun checkMainThread() {
        val cur = Thread.currentThread()
        val expect = mainThread ?: cur.also {
            mainThread = cur
        }
        check(cur === expect) { "Expected to be called on the main thread $expect, get $cur" }
    }

    /**
     * 投递任务到事件循环，非阻塞，可在任意线程调用。
     * 任务会在下一次 [runTurn] 或 [runBlocking] 的 pump 中被执行。
     */
    fun post(task: Runnable) {
        queue.offer(task)
        awake.trySend(Unit)
        check(mainThread?.isAlive != false) { "Main thread $mainThread is dead" }
    }

    /**
     * 在事件循环线程上执行「调用瞬间」积压的任务；执行期间新 [post] 的任务
     * 留到下一轮。
     *
     * 一般由平台 loader 在每次 tick 时调用，业务代码不应直接调用。
     */
    @LoaderApi
    fun runTurn() {
        checkMainThread()
        queue.offer(sentinel)           // 此刻之前的任务属于本轮
        while (true) {
            val item = queue.poll() ?: break
            if (item === sentinel) break   // 用 identity 判断，避免 equals 误判
            (item as Runnable).run()
        }
    }

    /**
     * 在事件循环线程上阻塞执行协程。
     *
     * 内部反复取任务执行，空闲时通过 Channel 挂起等待唤醒，
     * 从而驱动事件循环，让 block() 的挂起与恢复得以推进。
     */
    fun <T> runBlocking(block: suspend () -> T): T {
        checkMainThread()
        if (inBlocking) kotlinx.coroutines.runBlocking(MainEventLoop) { block() }
        inBlocking = true
        return kotlinx.coroutines.runBlocking {
            val sideJob = launch {
                while (inBlocking) {
                    when (val item = queue.poll()) {
                        null -> awake.receive()
                        is Runnable -> item.run()
                        else -> continue// 跳过边界标记，不是"空"
                    }
                }
            }
            try {
                withContext(MainEventLoop) { block() }
            } finally {
                inBlocking = false
                sideJob.cancelAndJoin()
            }
        }
    }
}