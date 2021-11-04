package coreLibrary

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "list", "列出所有模块或模块内所有脚本") {
            usage = "[module/fail]"
            permission = "scriptAgent.control.list"
            onComplete {
                onComplete(0) {
                    ScriptManager.allScripts { Config.isModule(it.id) }.map { it.id }
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
                    val list = ScriptManager.allScripts.filterValues {
                        if (module.equals("fail", true)) it.scriptState != ScriptState.Enable
                        else it.id.startsWith(module + Config.idSeparator)
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
            usage = "<module[/script]> [--noCache]"
            permission = "scriptAgent.control.reload"
            onComplete {
                onComplete(0) { ScriptManager.allScripts { true }.map { it.id } }
            }
            body {
                val noCache = checkArg("--noCache")
                if (arg.isEmpty()) replyUsage()
//                val bakFile = ScriptRegistry.idToSourceFile(arg[0]).let { source ->
//                    source.parentFile.listFiles()?.firstOrNull {
//                        it.name.endsWith(".bak") && it.name.startsWith(source.nameWithoutExtension)
//                    }
//                }
//                if (bakFile != null) {
//                    reply("[yellow]发现bak文件{name}".with("name" to bakFile.name))
//                    bakFile.renameTo(bakFile.parentFile.resolve(bakFile.nameWithoutExtension))
//                }
                val script = ScriptManager.getScriptNullable(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())
                if (noCache) {
                    val file = Config.cacheFile(script.id)
                    reply("[yellow]清理cache文件{name}".with("name" to file.name))
                    file.delete()
                }
                @Suppress("EXPERIMENTAL_API_USAGE")
                GlobalScope.launch {
                    reply("[yellow]异步处理中".with())
                    @OptIn(LoaderApi::class)
                    ScriptManager.loadRoot()
                    val success = ScriptManager.loadScript(script, force = true, enable = true, children = true) != null
                    reply((if (success) "[green]重载成功" else "[red]加载失败").with())
                }
            }
        })
        addSub(CommandInfo(thisRef, "disable", "关闭一个脚本或者模块") {
            usage = "<module[/script]> [--save]"
            permission = "scriptAgent.control.disable"
            onComplete {
                onComplete(0) { ScriptManager.allScripts { true }.map { it.id } }
            }
            body {
                val save = checkArg("--save")
                if (arg.isEmpty()) replyUsage()
                val script = ScriptManager.getScriptNullable(arg[0]) ?: returnReply("[red]找不到模块或者脚本".with())
                @Suppress("EXPERIMENTAL_API_USAGE")
                GlobalScope.launch {
                    reply("[yellow]异步处理中".with())
                    ScriptManager.disableScript(script, "手动关闭".takeIf { save })
                    reply("[green]关闭脚本成功".with())
//                    if (save) {
//                        val old = Config.realSourceFile(script.id) ?: return@launch
//                        val new = old.parentFile.resolve(old.name + ".bak")
//                        old.renameTo(new)
//                        reply("[yellow]重命名为{name}".with("name" to new.name))
//                    }
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}