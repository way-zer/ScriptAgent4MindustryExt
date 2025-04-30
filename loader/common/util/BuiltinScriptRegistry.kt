package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.define.SAExperimentalApi
import cf.wayzer.scriptAgent.define.ScriptSource
import java.net.URL

object BuiltinScriptRegistry : ScriptRegistry.IRegistry {
    @OptIn(SAExperimentalApi::class)
    class SourceImpl(meta: MetadataFile) : CASScriptSource(meta) {
        override fun getURL(hash: String): URL = javaClass.getResource("/builtin/CAS/$hash")
            ?: error("No builtin resource: $hash")
    }

    private val loaded by lazy {
        javaClass.getResourceAsStream("/builtin/META")?.reader()
            ?.useLines { MetadataFile.readAll(it.iterator()) }.orEmpty()
            .map { SourceImpl(it) }
            .also { Config.logger.info("BuiltinScriptRegistry found ${it.size} scripts") }
            .associateBy { it.id }
    }

    override fun scan(): Collection<ScriptSource> = loaded.values
}