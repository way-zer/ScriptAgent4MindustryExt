package coreLibrary

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "list", "列出所有模块或模块内所有脚本") {
            usage = "[module]"
            permission = "scriptAgent.control.list"
            onComplete {
                onComplete(0) {
                    ScriptManager.allScripts.keys.filter { Config.isModule(it) }
                }
            }
            body {
                fun Collection<ScriptInfo>.toReply(pad: Int) = map {
                    val enable = if (it.enabled) "purple" else "reset"
                    "[{enable}][{state}] {name} [blue]{desc}".with(
                        "enable" to enable, "state" to it.scriptState, "name" to it.id.padEnd(pad),
                        "desc" to (it.inst?.name ?: it.failReason)
                    )
                }
                if (arg.isEmpty()) {
                    val list = ScriptManager.allScripts.filterKeys { Config.isModule(it) }.values.toReply(20)
                    reply("[yellow]==== [light_yellow]已加载模块[yellow] ====\n{list:\n}".with("list" to list))
                } else {
                    val module = arg[0]
                    val list = ScriptManager.allScripts.filterKeys {
                        it.startsWith(module + Config.idSeparator)
                    }.values.toReply(30)
                    reply(
                        "[yellow]==== [light_yellow]{module}脚本[yellow] ====\n{list:\n}".with(
                            "module" to module, "list" to list
                        )
                    )
                }
            }
        })
        addSub(CommandInfo(thisRef, "reload", "重载一个脚本或者模块") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.reload"
            onComplete {
                onComplete(0) { ScriptManager.allScripts.keys.toList() }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptManager.getScriptNullable(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())
                GlobalScope.launch {
                    reply("[yellow]异步处理中".with())
                    val success = ScriptManager.loadScript(script, force = true, enable = true, children = true) != null
                    reply((if (success) "[green]重载成功" else "[red]加载失败").with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "disable", "关闭一个脚本或者模块") {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.disable"
            onComplete {
                onComplete(0) { ScriptManager.allScripts.keys.toList() }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptManager.getScriptNullable(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())
                GlobalScope.launch {
                    reply("[yellow]异步处理中".with())
                    ScriptManager.disableScript(script)
                    reply("[green]关闭脚本成功".with())
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}