package coreMindustry

import mindustry.core.ContentLoader
import mindustry.ctype.Content
import mindustry.ctype.MappableContent
import java.lang.reflect.Modifier
import kotlin.reflect.KMutableProperty0

val bakMap = mutableMapOf<KMutableProperty0<*>, Any?>()

/**Should invoke in [Dispatchers.game] */
fun <T : Content, R : T> newContent(origin: T, block: (origin: T) -> R): R {
    val bak = content
    content = object : ContentLoader() {
        override fun transformName(name: String?) = bak?.transformName(name) ?: name
        override fun handleContent(content: Content?) = Unit
        override fun handleMappableContent(content: MappableContent?) = Unit
    }
    return try {
        block(origin).also { new ->
            origin::class.java.fields.forEach {
                if (!it.declaringClass.isInstance(new)) return@forEach
                if (Modifier.isPublic(it.modifiers) && !Modifier.isFinal(it.modifiers)) {
                    it.set(new, it.get(origin))
                }
            }
        }
    } finally {
        content = bak
    }
}

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