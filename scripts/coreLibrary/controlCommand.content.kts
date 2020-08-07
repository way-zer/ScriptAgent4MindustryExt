package coreLibrary

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.IInitScript
import cf.wayzer.script_agent.ScriptManager

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "list", "列出所有模块或模块内所有脚本", {
            usage = "[module]"
            permission = "scriptAgent.control.list"
            supportCompletion = true
        }) {
            onComplete(0) {
                ScriptManager.loadedInitScripts.values.map { it.id }
            }
            endComplete()
            if (arg.isEmpty()) {
                val list = ScriptManager.loadedInitScripts.values.map {
                    val enable = if (it.enabled) "purple" else "reset"
                    "[{enable}]{name} [blue]{desc}\n".with(
                        "enable" to enable,
                        "name" to it.id.padEnd(20),
                        "desc" to it.name
                    )
                }
                return@CommandInfo reply(
                    """
                    [yellow]==== [light_yellow]已加载模块[yellow] ====
                    {list}
                """.trimIndent().with("list" to list)
                )
            }
            val module = arg[0].let(ScriptManager::getScript)?.let { it as? IInitScript }
                ?: return@CommandInfo reply("[red]找不到模块".with())
            val list = module.children.map {
                val enable = if (it.enabled) "purple" else "reset"
                "[{enable}]{name} [blue]{desc}\n".with(
                    "enable" to enable,
                    "name" to it.id.padEnd(30),
                    "desc" to it.name
                )
            }
            reply(
                """
                [yellow]==== [light_yellow]{module}脚本[yellow] ====
                {list}
            """.trimIndent().with("module" to module.name, "list" to list)
            )
        })
        addSub(CommandInfo(thisRef, "reload", "重载一个脚本或者模块", {
            usage = "<module[/script]>"
            permission = "scriptAgent.control.reload"
            supportCompletion = true
        }) {
            onComplete(0) {
                (arg[0].split('/')[0].let(ScriptManager::getScript)?.let { it as IInitScript }?.children
                    ?: ScriptManager.loadedInitScripts.values).map { it.id }
            }
            endComplete()
            if (arg.isEmpty()) return@CommandInfo replyUsage()
            GlobalScope.launch {
                reply("[yellow]异步处理中".with())
                val success: Boolean = when (val script = arg.getOrNull(0)?.let(ScriptManager::getScript)) {
                    is IInitScript -> ScriptManager.loadModule(script.sourceFile, force = true, enable = true) != null
                    is IContentScript -> ScriptManager.loadContent(
                        script.module, script.sourceFile,
                        force = true,
                        enable = true
                    ) != null
                    else -> return@launch reply("[red]找不到模块或者脚本".with())
                }
                reply((if (success) "[green]重载成功" else "[red]加载失败").with())
            }
        })
        addSub(CommandInfo(thisRef, "loadScript", "加载一个新脚本或者模块", {
            usage = "<filePath>"
            aliases = listOf("load")
            permission = "scriptAgent.control.load"
        }) {
            val file = arg.getOrNull(0)?.let(Config.rootDir::resolve)
                ?: return@CommandInfo reply("[red]未找到对应文件".with())
            GlobalScope.launch {
                reply("[yellow]异步处理中".with())
                val success: Boolean = when {
                    file.name.endsWith(Config.moduleDefineSuffix) -> ScriptManager.loadModule(
                        file,
                        enable = true
                    ) != null
                    file.name.endsWith(Config.contentScriptSuffix) -> {
                        val module = ScriptManager.getScript(arg[0].split('/')[0])
                        if (module !is IInitScript) return@launch reply("[red]找不到模块,请确定模块已先加载".with())
                        ScriptManager.loadContent(module, file, enable = true) != null
                    }
                    else -> return@launch reply("[red]不支持的文件格式".with())
                }
                reply((if (success) "[green]加载脚本成功" else "[red]加载失败,查看后台以了解详情").with())
            }
        })
        onDisable { removeAll(thisRef) }
    }
}