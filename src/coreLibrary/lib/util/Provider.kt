package coreLibrary.lib.util

import cf.wayzer.script_agent.IBaseScript

class Provider<T> {
    private var _data: T? = null
    private val onceListener = mutableSetOf<(T) -> Unit>()
    private val listener = mutableSetOf<(T) -> Unit>()
    fun once(body: (T) -> Unit) {
        if (_data == null) {
            onceListener.add(body)
        } else {
            body(_data!!)
        }
    }

    fun listenWithAutoCancel(script: IBaseScript, body: (T) -> Unit) {
        every(body)
        script.onDisable {
            cancel(body)
        }
    }

    fun every(body: (T) -> Unit) {
        listener.add(body)
        _data?.let(body)
    }

    fun cancel(body: (T) -> Unit): Boolean {
        return listener.remove(body)
    }

    fun set(v: T) {
        _data = v
        listener.forEach { it(v) }
        onceListener.forEach { it(v) }
        onceListener.clear()
    }

    fun get(): T? = _data

    @Throws(TypeCastException::class, NullPointerException::class)
    inline operator fun <reified G : T> invoke() = get()!! as G
}