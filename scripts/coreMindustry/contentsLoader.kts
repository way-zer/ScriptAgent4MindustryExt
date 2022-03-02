@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("cf.wayzer.MindustryContents:core:1.0.6", mavenDependsSingle = true)

package coreMindustry

import cf.wayzer.ContentsLoader
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency

val version by config.key("1.0.6", "MindustryContents版本", "重载生效")

val cLoader = javaClass.classLoader!!
DependencyManager {
    addRepository("https://www.jitpack.io/")
    requireWithChildren(Dependency.parse("cf.wayzer.MindustryContents:contents:$version"))
    loadToClassLoader(cLoader)
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
        Class.forName("Contents").apply {
            getMethod("register").invoke(kotlin.objectInstance)
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