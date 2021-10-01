package coreLibrary.lib.util

import cf.wayzer.scriptAgent.define.ISubScript
import cf.wayzer.scriptAgent.emit
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.event.ServiceProvidedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.properties.ReadOnlyProperty

/**
 * 模块化服务提供工具库
 */

@Suppress("unused")
open class ServiceRegistry<T : Any> {
    private val impl = MutableSharedFlow<T>(1, 0, BufferOverflow.DROP_OLDEST)

    fun provide(script: ISubScript, inst: T) {
        script.providedService.add(this to inst)
        this.impl.tryEmit(inst)
        ServiceProvidedEvent(inst, script).emit()
    }

    fun getOrNull() = impl.replayCache.firstOrNull()
    fun get() = getOrNull() ?: error("No Provider for ${this.javaClass.canonicalName}")

    val provided get() = getOrNull() != null
    fun toFlow() = impl.asSharedFlow()

    suspend fun awaitInit() = impl.first()
    fun subscribe(scope: CoroutineScope, body: suspend (T) -> Unit) {
        impl.onEach { body(it) }.launchIn(scope)
    }

    val nullable get() = ReadOnlyProperty<Any?, T?> { _, _ -> getOrNull() }
    val notNull get() = ReadOnlyProperty<Any?, T> { _, _ -> get() }

    companion object {
        val ISubScript.providedService by DSLBuilder.dataKeyWithDefault { mutableSetOf<Pair<ServiceRegistry<*>, *>>() }
    }
}