package coreLibrary.lib.util

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.loop(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> Unit) {
    launch(context) {
        while (true) {
            block()
        }
    }
}

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