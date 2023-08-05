package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.define.ScriptInfo
import cf.wayzer.scriptAgent.define.ScriptResourceFile
import cf.wayzer.scriptAgent.define.ScriptSource
import java.io.File
import java.net.URL

object BuiltinScriptRegistry : ScriptRegistry.IRegistry {
    fun getResUrl(md5: String): URL {
        return BuiltinScriptRegistry::class.java.getResource("/builtin/by_md5/$md5") ?: error(
            "No builtin resource: $md5"
        )
    }

    fun loadResFile(md5: String): File {
        return CAStore.getOrLoad(md5) { it.writeBytes(getResUrl(md5).readBytes()) }
    }

    class BuiltinScriptSource(
        override val scriptInfo: ScriptInfo,
        override val isModule: Boolean,
        private val scriptMd5: String,
        resources: Map<String, String>,
    ) : ScriptSource.Compiled {
        inner class BuiltinDBScriptResourceFile(
            override val name: String,
            private val md5: String
        ) : ScriptResourceFile {
            override val url: URL get() = getResUrl(md5)
            override fun loadFile(): File = loadResFile(md5)
        }

        private val resources = resources.mapValues { BuiltinDBScriptResourceFile(it.key, it.value) }
        override fun listResources(): Collection<ScriptResourceFile> = resources.values
        override fun findResource(name: String): ScriptResourceFile? = resources[name]

        override fun compiledValid(): Boolean = true
        override fun loadCompiled(): File = loadResFile(scriptMd5)
    }

    private var cache: Map<String, BuiltinScriptSource>? = null
    override fun findScriptSource(id: String): ScriptSource? = cache?.get(id)

    @Synchronized
    override fun scan(): List<ScriptSource> {
        //We only need scan once, as builtin won't update in runtime.
        if (cache == null) {
            val lines = BuiltinScriptRegistry::class.java.getResourceAsStream("/builtin/META")
                ?.reader()?.useLines { it.toList() }
            if (lines == null) {
                Config.logger.warning("BuiltinScriptRegistry found no scripts")
                cache = emptyMap()
                return emptyList()
            }
            //format: id(with moduleIdSuffix) md5 resources(split by ';')
            cache = lines.associate { line ->
                val (idStr, md5, resourcesStr) = line.split(' ')
                val isModule = idStr.endsWith(Config.moduleIdSuffix)
                val id = idStr.removeSuffix(Config.moduleIdSuffix)
                val resources = resourcesStr.takeUnless { it.isEmpty() }?.split(';')?.associate {
                    it.split(':').run { get(0) to get(1) }
                }.orEmpty()
                id to BuiltinScriptSource(ScriptInfo.getOrCreate(id), isModule, md5, resources)
            }
            Config.logger.info("BuiltinScriptRegistry found ${cache!!.size} scripts")
        }
        return cache!!.values.toList()
    }
}