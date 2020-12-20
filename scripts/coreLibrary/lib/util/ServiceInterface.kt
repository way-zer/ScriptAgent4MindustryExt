package coreLibrary.lib.util

import cf.wayzer.script_agent.ISubScript
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.event.ServiceProvidedEvent
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

/**
 * 模块化服务提供工具库
 */
interface ServiceInterface
abstract class ServerClass<T : ServiceInterface> {
    private var inst: WeakReference<T> = WeakReference(null)
    fun provide(script: ISubScript, inst: T) {
        script.providedService.add(this to inst)
        this.inst = WeakReference(inst)
        ServiceProvidedEvent(inst, script).emit()
    }

    fun getOrNull() = inst.get()
    fun get() = getOrNull() ?: error("No Provider for ${this.javaClass.canonicalName}")

    val provided get() = inst.get() != null
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        return get()
    }

    fun waitForProvider(script: ISubScript, once: Boolean = false, body: ServiceProvidedEvent<T>.() -> Unit) {
        var moreThanOnce = false
        script.listenTo<ServiceProvidedEvent<T>> {
            if (once && moreThanOnce)
                body()
            moreThanOnce = true
        }
    }

    companion object {
        val ISubScript.providedService by DSLBuilder.dataKeyWithDefault { mutableSetOf<Pair<ServerClass<out ServiceInterface>, ServiceInterface>>() }
    }
}