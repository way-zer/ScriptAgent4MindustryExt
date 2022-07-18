@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("cf.wayzer.MindustryContents:core:1.0.8", mavenDependsSingle = true)

package coreMindustry

import arc.util.serialization.Base64Coder
import cf.wayzer.ContentsLoader
import cf.wayzer.ContentsPatcher
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import java.security.MessageDigest

val version by config.key("1.0.8", "MindustryContents版本", "重载生效")

val cLoader = javaClass.classLoader!!
DependencyManager {
    addRepository("https://www.jitpack.io/")
    requireWithChildren(Dependency.parse("cf.wayzer.MindustryContents:contents:$version"))
    loadToClassLoader(cLoader)
}

listen<EventType.ResetEvent> {
    patches.clear()
    patchMap.clear()
    ContentsPatcher.Api.reset()

    ContentsLoader.Api.loadContent(mutableListOf())
    ContentsLoader.Api.lastLoadedPacks.forEach {
        Call.clientPacketReliable("ContentsLoader|load", it)
    }
    registerVar("scoreBroad.ext.contentsVersion", "内容包版本显示", ("[violet]资源包已加载: [orange]$version\n" +
            "  [yellow](使用[sky]ContentsLoader[]MOD获得最佳体验)").takeIf {
        ContentsLoader.Api.lastLoadedPacks != listOf("origin") || patches.isNotEmpty()
    })
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

//patch
@Savable(false)
val patches = mutableListOf<String>()//md5
customLoad(::patches, patches::addAll)

@Savable(false)
val patchMap = mutableMapOf<String, String>()//md5->text
customLoad(::patchMap, patchMap::putAll)

val md5Digest = MessageDigest.getInstance("md5")!!
fun addPatch(patch: String) {
    val md5 = Base64Coder.encode(md5Digest.digest(patch.toByteArray())).concatToString()
    patchMap[md5] = patch
    patches.add(md5)
    Call.clientPacketReliable("ContentsLoader|loadPatch", md5)
    ContentsPatcher.Api.load(patch)
}
export(::addPatch)

onEnable {
    netServer.addPacketHandler("ContentsLoader|requestPatch") { p, msg ->
        patchMap[msg]?.let {
            Call.clientPacketReliable(p.con, "ContentsLoader|newPatch", msg + "\n" + it)
        }
    }
}

onDisable {
    netServer.getPacketHandlers("ContentsLoader|requestPatch").clear()
}

listen<EventType.PlayerJoin> {
    patches.forEach {
        Call.clientPacketReliable("ContentsLoader|loadPatch", it)
    }
}