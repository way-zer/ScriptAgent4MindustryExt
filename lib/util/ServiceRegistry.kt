package coreLibrary.lib.util

import cf.wayzer.scriptAgent.define.ISubScript
import cf.wayzer.scriptAgent.emit
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.event.ServiceProvidedEvent
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlin.properties.ReadOnlyProperty

/**
 * 模块化服务提供工具库
 */

@Suppress("unused")
@OptIn(ObsoleteCoroutinesApi::class)
open class ServiceRegistry<T : Any> {
    private val store = ConflatedBroadcastChannel<T>()
    fun provide(script: ISubScript, inst: T) {
        script.providedService.add(this to inst)
        this.store.trySend(inst)
        ServiceProvidedEvent(inst, script).emit()
    }

    fun getOrNull() = store.valueOrNull
    fun get() = getOrNull() ?: error("No Provider for ${this.javaClass.canonicalName}")

    val provided get() = getOrNull() != null
    fun toChannel() = store.openSubscription()
    suspend fun subscribe(body: (T) -> Unit) {
        store.openSubscription().consumeEach(body)
    }

    suspend fun waitForProvider(body: (T) -> Unit) {
        store.openSubscription().consume {
            body(receive())
        }
    }

    val nullable get() = ReadOnlyProperty<Any?, T?> { _, _ -> getOrNull() }
    val notNull get() = ReadOnlyProperty<Any?, T> { _, _ -> get() }

    companion object {
        val ISubScript.providedService by DSLBuilder.dataKeyWithDefault { mutableSetOf<Pair<ServiceRegistry<*>, *>>() }
    }
}