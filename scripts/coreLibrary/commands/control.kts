package coreLibrary.commands

import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.scriptAgent.state.ConditionState

suspend inline fun runIgnoreCancel(sync: Boolean, crossinline body: suspend () -> Unit) {
    //Need new Job, as it may restart this script.
    @Suppress("CoroutineContextWithJob")
    val job = launch(Job()) { body() }
    if (sync) job.join()
}

command("scan", "重新扫描脚本".with(), commands = Commands.controlCommand) {
    requirePermission("scriptAgent.control.scan")
    aliases = listOf("扫描")
    body {
        val old = ScriptRegistry.allScripts { true }.toSet()
        ScriptRegistry.scanRoot()
        val now = ScriptRegistry.allScripts { true }.toSet()
        reply(
            "[green]扫描完成,新增{added}脚本, 删除{removed}脚本".with(
                "added" to (now - old).size,
                "removed" to (old - now).size,
            )
        )
    }
}
command("listFailed", "列出所有故障脚本".with(), commands = Commands.controlCommand) {
    usage = "[prefix]"
    requirePermission("scriptAgent.control.list")
    aliases = listOf("fail", "failed")
    onComplete {
        onComplete(0) {
            ScriptRegistry.allScripts().map { it.id.substringBefore(Config.idSeparator) }
                .toSet().sortedBy { it }
        }
    }
    body {
        val prefix = arg.firstOrNull().orEmpty()
        val scripts = ScriptRegistry.allScripts {
            it.id.startsWith(prefix) && !it.ready()
        }
        for (info in scripts) {
            reply(buildString {
                appendLine("[${info.scriptState}] ${info.id}")
                info.lastConditions.filter { it.status != ConditionState.Status.Success }.forEach { c ->
                    c.display().forEach {
                        append("  ")
                        appendLine(it)
                    }
                }
                deleteAt(length - 1)
            }.asPlaceHoldString())
        }
    }
}
command("list", "列出所有模块或模块内所有脚本".with(), commands = Commands.controlCommand) {
    usage = "[module]"
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
        if (module.equals("fail", true)) returnReply("[red]请使用新指令/sa fail [prefix]".with())
        val list = ScriptRegistry.allScripts {
            it.id.startsWith(module + Config.idSeparator)
        }.map { script ->
            val color = if (script.ready()) "green" else "red"
            val conditions = script.lastConditions.filter { it.status != ConditionState.Status.Success }
                .joinToString("") { "${it.type}${it.status}" }
            "[$color]${script.id.padEnd(30)}[reset] [${script.scriptState}] $conditions"
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
        var noEnable = checkArg("--noEnable")
        val async = checkArg("--async")

        if (arg.isEmpty()) replyUsage()
        val script = ScriptRegistry.getScriptInfo(arg[0])
            ?: returnReply("[red]找不到模块或者脚本".with())
        runIgnoreCancel(!async) {
            ScriptManager.transactionV2 {
                if (script.scriptState.loaded) {
                    reload(script)
                } else if (!noEnable) {
                    enable(script)
                } else {
                    load(script)
                }
            }.printResult()
        }
    }
}
command("compile", "(实验性)编译单个脚本(不影响运行中实例)".with(), commands = Commands.controlCommand) {
    usage = "<module[/script]> [--async]"
    requirePermission("scriptAgent.control.compile")
    aliases = listOf("编译")
    onComplete {
        onComplete(0) { ScriptRegistry.allScripts { true }.map { it.id } }
    }
    body {
        val async = checkArg("--async")

        if (arg.isEmpty()) replyUsage()
        val script = ScriptRegistry.getScriptInfo(arg[0])
            ?: returnReply("[red]找不到模块或者脚本".with())
        runIgnoreCancel(!async) {
            ScriptManager.transactionV2 {
                compile(script)
            }.printResult()
        }
    }
}
command("retry", "(实验性)重试事务".with(), commands = Commands.controlCommand) {
    usage = "[--async]"
    requirePermission("scriptAgent.control.retry")
    aliases = listOf("重试")
    body {
        val async = checkArg("--async")
        runIgnoreCancel(!async) {
            ScriptManager.transactionV2 {
                ScriptRegistry.allScripts { !it.ready() }.forEach {
                    compile(it)
                }
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