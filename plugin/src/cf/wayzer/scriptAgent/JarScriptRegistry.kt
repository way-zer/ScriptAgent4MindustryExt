package cf.wayzer.scriptAgent

import cf.wayzer.scriptAgent.define.ScriptSource
import java.io.InputStream
import java.net.URL

object JarScriptRegistry : ScriptRegistry.IRegistry {
    @Suppress("MemberVisibilityCanBePrivate")
    class JarScriptSource(id: String, val url: URL, override val isModule: Boolean) : ScriptSource(id) {
        override val isKtc: Boolean get() = true
        override fun load(): InputStream {
            return url.openStream()
        }

        override fun resolveRes(path: String): URL? {
            return if (isModule) javaClass.classLoader.getResource("scripts/$id/res/$path")
            else javaClass.classLoader.getResource("scripts/$id.$path")
        }
    }

    private val all: Map<String, ScriptSource> by lazy {
        val classloader = javaClass.classLoader
        val list = classloader.getResourceAsStream("scripts/PACKED")?.use {
            it.reader().readLines()
        } ?: return@lazy emptyMap()
        list.mapNotNull { fileName ->
            val url = classloader.getResource("scripts/$fileName")
            var id = fileName.removeSuffix(".ktc")
            val isModule = id.endsWith("/module")
            if (isModule) id = id.removeSuffix("/module")
            url?.run { id to JarScriptSource(id, url, isModule) }
        }.toMap()
    }

    override fun findScriptSource(id: String): ScriptSource? = all[id]

    override fun scan(): List<ScriptSource> {
        return all.values.toList()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(
            javaClass.classLoader.getResources("plugin.json").toList()
        )
    }
}