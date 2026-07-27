package coreLibrary.lib.util

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

fun Script.loop(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit) {
    launch(context) {
        while (true) {
            try {
                block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.log(Level.WARNING, "Exception inside loop, auto sleep 10s.", e)
                delay(10.seconds)
            }
        }
    }
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Only for script")
fun CoroutineScope.loop(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit) =
    (this as Script).loop(context, block)

@ScriptUtil
inline fun <T> Script.withContextClassloader(loader: ClassLoader = javaClass.classLoader, block: () -> T): T {
    val bak = Thread.currentThread().contextClassLoader
    return try {
        Thread.currentThread().contextClassLoader = loader
        block()
    } finally {
        Thread.currentThread().contextClassLoader = bak
    }
}