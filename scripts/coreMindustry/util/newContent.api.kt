package coreMindustry.util

import coreMindustry.lib.game
import kotlinx.coroutines.Dispatchers
import mindustry.Vars.content
import mindustry.core.ContentLoader
import mindustry.ctype.Content
import mindustry.ctype.MappableContent
import java.lang.reflect.Modifier

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