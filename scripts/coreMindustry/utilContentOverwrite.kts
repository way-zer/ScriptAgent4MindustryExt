@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("cf.wayzer.MindustryContents:core:1.0.5", mavenDependsSingle = true)
package coreMindustry

import cf.wayzer.ContentsLoader
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

listen<EventType.ResetEvent> {
    ContentsLoader.Api.loadContent(mutableListOf())
    ContentsLoader.Api.lastLoadedPacks.forEach {
        Call.clientPacketReliable("ContentsLoader|load", it)
    }
}

listen<EventType.ConnectionEvent> { e ->
    ContentsLoader.Api.lastLoadedPacks.forEach {
        Call.clientPacketReliable(e.connection, "ContentsLoader|load", it)
    }
}

onEnable {
    ContentsLoader.Api.apply {
        logTimeCost = { tag, time ->
            logger.info("$tag costs ${time}ms")
        }
    }
    content = ContentsLoader
    netServer.addPacketHandler("ContentsLoader|load") { p, msg ->
        logger.info("${p.name}(${p.uuid()}): $msg")
    }
}

onDisable {
    content = ContentsLoader.origin
    netServer.getPacketHandlers("ContentsLoader|load").clear()
}