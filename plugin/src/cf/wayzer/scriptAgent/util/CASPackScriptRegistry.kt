package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.define.SAExperimentalApi
import cf.wayzer.scriptAgent.define.ScriptSource
import java.io.File
import java.net.URL

@SAExperimentalApi
object CASPackScriptRegistry : ScriptRegistry.IRegistry {
    val files = mutableSetOf<File>()
    private var cache = emptyMap<String, SourceImpl>()

    class SourceImpl(var file: File, meta: MetadataFile) : CASScriptSource(meta) {
        override fun getURL(hash: String): URL? =
            if (!file.exists()) null else URL("jar:${file.toURI()}!/CAS/$hash")
    }

    override fun scan(): Collection<ScriptSource> {
        files.removeIf { !it.exists() }
        files += Config.rootDir.listFiles().orEmpty().filter { it.name.endsWith(".packed.zip") }
        if (files.size > 0) println("found packed files: ${files.joinToString { it.name }}")
        cache = buildMap {
            for (file in files) {
                val metas = URL("jar:${file.toURI()}!/META")
                    .openStream().bufferedReader().use { it.readLines() }
                    .let { MetadataFile.readAll(it.iterator()) }
                for (meta in metas) {
                    if (meta.id in this) continue
                    val source = SourceImpl(file, meta)
                    //reuse if not changed
                    this[source.id] = cache[source.id]
                        ?.takeIf { it.hash == source.hash && it.resourceHashes == source.resourceHashes }
                        ?.also { it.file = file } ?: source
                }
            }
        }
        return cache.values
    }
}