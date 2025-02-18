package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.define.SAExperimentalApi
import cf.wayzer.scriptAgent.define.ScriptInfo
import cf.wayzer.scriptAgent.define.ScriptResourceFile
import cf.wayzer.scriptAgent.define.ScriptSource
import java.io.File
import java.net.URL

@SAExperimentalApi
abstract class CASScriptSource(
    override val scriptInfo: ScriptInfo,
    val hash: String,
    val resourceHashes: Map<String, String>,
) : ScriptSource.Compiled {
    abstract fun getURL(hash: String): URL?
    class ResourceImpl(
        override val name: String,
        private val hash: String,
        private val originUrl: URL
    ) : ScriptResourceFile {
        override val url: URL get() = CAStore.get(hash)?.toURI()?.toURL() ?: originUrl
        override fun loadFile(): File = CAStore.getOrLoad(hash, originUrl)
    }

    override fun listResources(): Collection<ScriptResourceFile> = resourceHashes
        .mapNotNull { getURL(it.value)?.let { url -> ResourceImpl(it.key, it.value, url) } }

    override fun findResource(name: String): ScriptResourceFile? {
        val hash = resourceHashes[name] ?: return null
        val url = getURL(hash) ?: return null
        return ResourceImpl(name, hash, url)
    }


    override fun compiledValid(): Boolean = getURL(hash) != null
    override fun loadCompiled(): File = getURL(hash)?.let { CAStore.getOrLoad(hash, it) }
        ?: error("Can't load compiled script")

    constructor(meta: MetadataFile) : this(
        ScriptInfo.getOrCreate(meta.id),
        meta.attr["HASH"] ?: error("Break META: ${meta.id}, require hash"),
        meta.data["RESOURCE"].orEmpty().associate {
            val (name, hash) = it.split(' ', limit = 2)
            name to hash
        }
    )
}