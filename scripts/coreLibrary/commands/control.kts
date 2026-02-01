package coreLibrary.commands

import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.scriptAgent.state.ConditionState

suspend inline fun runIgnoreCancel(sync: Boolean, crossinline body: suspend () -> Unit) {
    val job = launch(Job()) { body() }
    if (sync) job.join()
}

command("scan", "重新扫描脚本".with(), commands = Commands.controlCommand) {
    requirePermission("scriptAgent.control.scan")
    aliases = listOf("扫描")
    body {
        val old = ScriptRegistry.allScripts { true }.size
        ScriptRegistry.scanRoot()
        val now = ScriptRegistry.allScripts { true }.size
        reply("[green]扫描完成,新发现{count}脚本".with("count" to (now - old)))
    }
}
command("list", "列出所有模块或模块内所有脚本".with(), commands = Commands.controlCommand) {
    usage = "[module/fail]"
    requirePermission("scriptAgent.control.list")
    aliases = listOf("ls", "列出")
    onComplete {
        onComplete(0) {
            ScriptRegistry.allScripts().map { it.id.substringBefore(Config.idSeparator) }
                .toSet().sortedBy { it }
        }
    }
    body {
        val module = arg.getOrNull(0) ?: kotlin.run {
            val counts = ScriptRegistry.allScripts().map { it.id.substringBefore(Config.idSeparator) }
                .groupBy { it }.mapValues { it.value.size }
            val list = counts.entries.sortedBy { it.key }
                .map { "[purple]${it.key.padEnd(20)} [blue]${it.value}" }
            returnReply("[yellow]==== [light_yellow]已加载模块[yellow] ====\n{list|joinLines}".with("list" to list))
        }
        val list = ScriptRegistry.allScripts {
            if (module.equals("fail", true)) it.conditions.any { c -> c.status != ConditionState.Status.Success }
            else it.id.startsWith(module + Config.idSeparator)
        }.map {
            if (it.enabled) "[purple][${it.scriptState}] ${it.id}"
            else "[reset][${it.scriptState}] ${it.id.padEnd(30)} ${it.conditions}"
        }
        reply(
            "[yellow]==== [light_yellow]{module}脚本[yellow] ====\n{list|joinLines}".with(
                "module" to module, "list" to list
            )
        )
    }
}
command("load", "(重新)加载一个脚本或者模块".with(), commands = Commands.controlCommand) {
    usage = "<module[/script]> [--noCache] [--noEnable] [--async]"
    requirePermission("scriptAgent.control.load")
    aliases = listOf("reload", "加载", "重载")
    onComplete {
        onComplete(0) { ScriptRegistry.allScripts { true }.map { it.id } }
    }
    body {
        val noCache = checkArg("--noCache")
        var noEnable = checkArg("--noEnable")
        val async = checkArg("--async")

        if (arg.isEmpty()) replyUsage()
        val script = ScriptRegistry.getScriptInfo(arg[0])
            ?: returnReply("[red]找不到模块或者脚本".with())
        if (noCache) {
            val file = Config.cacheFile(script.id)
            reply("[yellow]清理cache文件{name}".with("name" to file.name))
            file.delete()
        }
        runIgnoreCancel(!async) {
            ScriptManager.transactionV2 {
                unload(script)
                execute()
                (if (noEnable) load else enable)(script)
            }.printResult()
        }
    }
}
command("enable", "(重新)启用一个脚本或者模块".with(), commands = Commands.controlCommand) {
    usage = "<module[/script]> [--async]"
    requirePermission("scriptAgent.control.enable")
    aliases = listOf("启用")
    onComplete {
        onComplete(0) { ScriptRegistry.allScripts { it.scriptState.loaded }.map { it.id } }
    }
    body {
        val async = checkArg("--async")
        if (arg.isEmpty()) replyUsage()
        val script = ScriptRegistry.getScriptInfo(arg[0])
            ?: returnReply("[red]找不到模块或者脚本".with())
        runIgnoreCancel(!async) {
            ScriptManager.transactionV2 {
                disable(script)
                execute()
                enable(script)
            }.printResult()
        }
    }
}
command("unload", "卸载一个脚本或者模块".with(), commands = Commands.controlCommand) {
    usage = "<module[/script]> [--async]"
    requirePermission("scriptAgent.control.unload")
    aliases = listOf("卸载")
    onComplete {
        onComplete(0) { ScriptRegistry.allScripts { it.scriptState.loaded }.map { it.id } }
    }
    body {
        val async = checkArg("--async")
        if (arg.isEmpty()) replyUsage()
        val script = ScriptRegistry.getScriptInfo(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())

        runIgnoreCancel(!async) {
            ScriptManager.unloadScript(script)
            reply("[green]关闭脚本成功".with())
        }
    }
}
command("disable", "关闭一个脚本或者模块".with(), commands = Commands.controlCommand) {
    usage = "<module[/script]> [--async]"
    requirePermission("scriptAgent.control.disable")
    aliases = listOf("关闭")
    onComplete {
        onComplete(0) { ScriptRegistry.allScripts { it.scriptState.enabled }.map { it.id } }
    }
    body {
        val async = checkArg("--async")
        if (arg.isEmpty()) replyUsage()
        val script = ScriptRegistry.getScriptInfo(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())

        runIgnoreCancel(!async) {
            ScriptManager.disableScript(script)
            reply("[green]关闭脚本成功".with())
        }
    }
}