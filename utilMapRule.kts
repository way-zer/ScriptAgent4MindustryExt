package coreMindustry

import mindustry.content.UnitTypes
import kotlin.reflect.KMutableProperty0

val bakMap = mutableMapOf<KMutableProperty0<*>, Any?>()

fun <T> registerMapRule(field: KMutableProperty0<T>, valueFactory: (T) -> T) {
    synchronized(bakMap) {
        @Suppress("UNCHECKED_CAST")
        val old = (bakMap[field] as T?) ?: field.get()
        val new = valueFactory(old)
        if (new === old)
            error("valueFactory can't return the same instance for $field")
        field.set(new)
        bakMap[field] = old
    }
}

listen<EventType.ResetEvent> {
    synchronized(bakMap) {
        bakMap.forEach { (field, bakValue) ->
            @Suppress("UNCHECKED_CAST")
            (field as KMutableProperty0<Any?>).set(bakValue)
        }
        bakMap.clear()
    }
}

registerMapRule(UnitTypes.gamma::health) { 2000f }