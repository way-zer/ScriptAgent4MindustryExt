package coreLibrary

import cf.wayzer.scriptAgent.impl.ScriptCache

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "scan", "重新扫描脚本") {
            permission = "scriptAgent.control.scan"
            aliases = listOf("扫描")
            body {
                val old = ScriptRegistry.allScripts { true }.size
                ScriptRegistry.scanRoot()
                val now = ScriptRegistry.allScripts { true }.size
                reply("[green]扫描完成,新发现{count}脚本".with("count" to (now - old)))
            }
        })
        addSub(CommandInfo(thisRef, "list", "列出所有模块或模块内所有脚本") {
            usage = "[module/fail]"
            permission = "scriptAgent.control.list"
            aliases = listOf("ls", "列出")
            onComplete {
                onComplete(0) {
                    ScriptRegistry.allScripts { it.source.isModule }.map { it.id }
                }
            }
            body {
                fun Collection<ScriptInfo>.toReply(pad: Int) = map {
                    val enable = if (it.enabled) "purple" else "reset"
                    "[{enable}][{state}] {name} [blue]{desc}".with(
                        "enable" to enable, "state" to it.scriptState, "name" to it.id.padEnd(pad),
                        "desc" to (it.failReason ?: it.inst?.name ?: "")
                    )
                }
                if (arg.isEmpty()) {
                    val list = ScriptRegistry.allScripts { it.source.isModule }.toReply(20)
                    reply("[yellow]==== [light_yellow]已加载模块[yellow] ====\n{list:\n}".with("list" to list))
                } else {
                    val module = arg[0]
                    val list = ScriptRegistry.allScripts {
                        if (module.equals("fail", true)) it.failReason != null
                        else it.id.startsWith(module + Config.idSeparator)
                    }.toReply(30)
                    reply(
                        "[yellow]==== [light_yellow]{module}脚本[yellow] ====\n{list:\n}".with(
                            "module" to module, "list" to list
                        )
                    )
                }
            }
        })
        addSub(CommandInfo(thisRef, "load", "(重新)加载一个脚本或者模块") {
            usage = "<module[/script]> [--noCache] [--noEnable]"
            permission = "scriptAgent.control.load"
            aliases = listOf("reload", "加载", "重载")
            onComplete {
                onComplete(0) { ScriptRegistry.allScripts { true }.map { it.id } }
            }
            body {
                val noCache = checkArg("--noCache")
                var noEnable = checkArg("--noEnable")
                if (arg.isEmpty()) replyUsage()
                val script = ScriptRegistry.findScriptInfo(arg[0])
                    ?: returnReply("[red]找不到模块或者脚本".with())
                if (noCache) {
                    val file = Config.cacheFile(script.id, script.source.isModule)
                    reply("[yellow]清理cache文件{name}".with("name" to file.name))
                    file.delete()
                }
                //not cancel when disable self
                launch(Job()) {
                    reply("[yellow]异步处理中".with())
                    ScriptManager.transaction {
                        add(script)
                        if (script.failReason == null && !script.enabled)//因为其他原因本来就保持loaded
                            noEnable = true
                        unload(addAllAffect = true)
                        load()
                        if (!noEnable) enable()
                    }
                    script.failReason?.let {
                        reply("[red]加载失败({state}): {reason}".with("state" to script.scriptState, "reason" to it))
                    } ?: reply("[green]重载成功: {state}".with("state" to script.scriptState))
                }
            }
        })
        addSub(CommandInfo(thisRef, "enable", "(重新)启用一个脚本或者模块") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.enable"
            aliases = listOf("启用")
            onComplete {
                onComplete(0) { ScriptRegistry.allScripts { it.scriptState.loaded }.map { it.id } }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptRegistry.getScriptInfo(arg[0])
                    ?: returnReply("[red]找不到模块或者脚本".with())
                //not cancel when disable self
                launch(Job()) {
                    reply("[yellow]异步处理中".with())
                    ScriptManager.transaction {
                        add(script)
                        disable(addAllAffect = true)
                        enable()
                    }
                    val success = script.scriptState.enabled
                    reply((if (success) "[green]启用成功" else "[red]加载失败").with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "unload", "卸载一个脚本或者模块") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.unload"
            aliases = listOf("卸载")
            onComplete {
                onComplete(0) { ScriptRegistry.allScripts { it.scriptState.loaded }.map { it.id } }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptRegistry.getScriptInfo(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())

                //not cancel when disable self
                launch(Job()) {
                    reply("[yellow]异步处理中".with())
                    ScriptManager.unloadScript(script)
                    if (script.scriptState == ScriptState.ToLoad)
                        script.scriptInfo.stateUpdate(ScriptState.Found)
                    reply("[green]关闭脚本成功".with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "disable", "关闭一个脚本或者模块") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.disable"
            aliases = listOf("关闭")
            onComplete {
                onComplete(0) { ScriptRegistry.allScripts { it.scriptState.enabled }.map { it.id } }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptRegistry.getScriptInfo(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())

                //not cancel when disable self
                launch(Job()) {
                    reply("[yellow]异步处理中".with())
                    ScriptManager.disableScript(script)
                    if (script.scriptState == ScriptState.ToEnable)
                        script.scriptInfo.stateUpdate(ScriptState.Loaded)
                    reply("[green]关闭脚本成功".with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "genMetadata", "生成供开发使用的元数据") {
            permission = "scriptAgent.control.genMetadata"
            body {
                launch {
                    val all = ScriptRegistry.allScripts { it.children(true).isNotEmpty() }
                        .mapNotNull { it.compiledScript }
                    reply("[yellow]异步处理,共{size}待生成".with("size" to all.size))
                    Config.metadataDir.mkdirs()
                    all.forEach { info ->
                        Config.metadataFile(info.scriptInfo.id).writer().use {
                            ScriptCache.asMetadata(info).writeTo(it)
                        }
                    }
                    reply("[green]生成完成".with())
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}