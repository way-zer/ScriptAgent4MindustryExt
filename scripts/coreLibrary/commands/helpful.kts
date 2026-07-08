package coreLibrary.commands

import cf.wayzer.scriptAgent.impl.ScriptCache
import cf.wayzer.scriptAgent.util.CASScriptPacker
import cf.wayzer.scriptAgent.util.MetadataFile
import java.io.File

private fun File.moduleId(rootDir: File): String? {
    return relativeTo(rootDir).invariantSeparatorsPath
        .takeIf { it.isNotEmpty() && it != "." }
}

fun scanModules(extDir: List<String>): List<String> = buildSet {
    Config.rootDir.walkTopDown()
        .filter { it.isFile && it.name == ".metadata" }
        .mapNotNull { it.parentFile.moduleId(Config.rootDir) }
        .forEach(this::add)

    extDir.forEach { ext ->
        ext.replace('\\', '/')
            .trim('/')
            .takeIf { it.isNotEmpty() }
            ?.let { add(it) }
    }
}.sorted()

fun getModuleScripts(module: String, allModules: List<String>): Map<String, MetadataFile> {
    val scripts = ScriptRegistry.allScripts { it.id == module || it.id.startsWith("$module/") }
        .mapNotNull { it.compiledScript }.toMutableList()
    //排除子模块，避免重复
    val childModules = allModules.filter { it.startsWith("$module/") }
    childModules.forEach { child ->
        scripts.removeAll { it.id == child || it.id.startsWith("$child/") }
    }
    return scripts.associate { info ->
        val meta = ScriptCache.asMetadata(info)
        //不需要 SOURCE_MD5
        meta.id to MetadataFile(meta.id, meta.attr - "SOURCE_MD5", meta.data)
    }
}

command("genMetadata", "生成供开发使用的元数据".with(), commands = Commands.controlCommand) {
    permission = "scriptAgent.control.genMetadata"
    usage = "[moduleDir...]"
    body {
        withContext(Dispatchers.Default) {
            val modules = scanModules(arg)

            modules.forEach { moduleId ->
                val dir = Config.rootDir.resolve(moduleId)
                if (!dir.exists()) {
                    reply("[yellow]模块文件夹未找到 {id}, 跳过".with("id" to moduleId))
                    return@forEach
                }

                val metas = getModuleScripts(moduleId, modules)

                //读取已存在的，保留已有的
                val metadataFile = Config.metadataV3(dir)
                val existed = metadataFile.takeIf { it.isFile }?.let { file ->
                    file.reader().useLines { lines ->
                        MetadataFile.readAll(lines.iterator())
                    }.associateBy { it.id }
                }.orEmpty()

                val merged = existed + metas
                reply("[green]模块{id}: {size}".with("id" to moduleId, "size" to merged.size))
                metadataFile.writer().use {
                    merged.values.sortedBy { meta -> meta.id }.forEach { meta ->
                        meta.writeTo(it)
                    }
                }
            }
            reply("[green]生成完成".with())
        }
    }
}
command("packModule", "打包模块".with(), commands = Commands.controlCommand) {
    usage = "<module>"
    permission = "scriptAgent.control.packModule"
    body {
        val module = arg.getOrNull(0) ?: replyUsage()
        val scripts = ScriptRegistry.allScripts { it.id.startsWith("$module/") }
            .mapNotNull { it.compiledScript }
        CASScriptPacker(Config.cacheDir.resolve("$module.packed.zip").outputStream())
            .use { scripts.forEach(it::add) }
    }
}
