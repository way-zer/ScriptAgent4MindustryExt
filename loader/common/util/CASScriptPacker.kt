package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.define.SAExperimentalApi
import cf.wayzer.scriptAgent.impl.SACompiledScript
import cf.wayzer.scriptAgent.impl.ScriptCache
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@SAExperimentalApi
class CASScriptPacker(stream: OutputStream) : AutoCloseable {
    val metas = mutableListOf<MetadataFile>()
    private val added = mutableSetOf<String>()
    private val digest = MessageDigest.getInstance("MD5")!!
    private val zip = ZipOutputStream(stream)

    fun addCAS(bs: ByteArray): String {
        @OptIn(ExperimentalStdlibApi::class)
        val hash = digest.digest(bs).toHexString()
        if (added.add(hash)) {
            zip.putNextEntry(ZipEntry("CAS/$hash"))
            zip.write(bs)
            zip.closeEntry()
        }
        return hash
    }

    override fun close() {
        zip.putNextEntry(ZipEntry("META"))
        zip.bufferedWriter().let { f ->
            metas.sortedBy { it.id }
            metas.forEach { it.writeTo(f) }
            f.flush()
        }
        zip.closeEntry()
        zip.close()
    }

    fun add(script: SACompiledScript) {
        val scriptMD5 = addCAS(script.compiledFile.readBytes())
        val resources = script.source.listResources()
            .map { it.name to addCAS(it.loadFile().readBytes()) }
            .sortedBy { it.first }
            .map { "${it.first} ${it.second}" }

        val meta = ScriptCache.asMetadata(script)
        metas += meta.copy(
            attr = meta.attr + ("HASH" to scriptMD5),
            data = meta.data + ("RESOURCE" to resources)
        )
    }
}