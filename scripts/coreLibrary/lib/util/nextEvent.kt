package coreLibrary.lib.util

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.listenTo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface ReceivedEvent {
    var received: Boolean
}

suspend inline fun <reified T : Event> Script.nextEvent(crossinline filter: (T) -> Boolean): T =
    suspendCancellableCoroutine {
        lateinit var listen: Event.Listen<T>
        listen = listenTo {
            if (filter(this)) {
                if (this is ReceivedEvent) received = true
                listen.unregister()
                it.resume(this)
            }
        }
        it.invokeOnCancellation { listen.unregister() }
    }