package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.Config
import java.io.File

/** Contents Addressed Store*/
object CAStore {
    private val base = Config.cacheDir.resolve("by_md5").also { it.mkdirs() }
    fun getUncheck(md5: String) = base.resolve(md5)
    fun get(md5: String) = base.resolve(md5).takeIf { it.exists() }
    inline fun getOrLoad(md5: String, loader: (File) -> Unit): File {
        val file = getUncheck(md5)
        if (!file.exists()) {
            loader(file)
        }
        return file
    }
}