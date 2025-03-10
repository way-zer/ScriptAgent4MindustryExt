package coreLibrary.commands

import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.scriptAgent.impl.ScriptCache
import cf.wayzer.scriptAgent.util.CASScriptPacker
import cf.wayzer.scriptAgent.util.MetadataFile

command("genMetadata", "生成供开发使用的元数据".with(), commands = Commands.controlCommand) {
    permission = "scriptAgent.control.genMetadata"
    body {
        withContext(Dispatchers.Default) {
            val grouped = ScriptRegistry.allScripts().mapNotNull { it.compiledScript }
                .groupBy { it.id.substringBefore(Config.idSeparator) }
            Config.metadataDir.mkdirs()
            grouped.forEach { (id, group) ->
                reply("[yellow]模块{id}: {size}".with("id" to id, "size" to group.size))
                Config.metadataFile(id).writer().use {
                    group.sortedBy { it.id }.forEach { info ->
                        val meta = ScriptCache.asMetadata(info)
                        MetadataFile(meta.id, meta.attr - "SOURCE_MD5", meta.data).writeTo(it)
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
        @OptIn(SAExperimentalApi::class)
        CASScriptPacker(Config.cacheDir.resolve("$module.packed.zip").outputStream())
            .use { scripts.forEach(it::add) }
    }
}