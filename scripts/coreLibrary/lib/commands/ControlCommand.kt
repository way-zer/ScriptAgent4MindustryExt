package coreLibrary.lib.commands

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IBaseScript
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.IInitScript
import coreLibrary.lib.ICommand
import coreLibrary.lib.ICommands
import coreLibrary.lib.ISender
import coreLibrary.lib.with

class ControlCommand<S : ISender<*>>(val checkPermission: S.() -> Boolean) :
    ICommands<S>(null, "ScriptAgent", "ScriptAgent 控制指令", listOf("sa")) {
    private val manager = Config.inst

    init {
        addSub(ICommand(null, "list", "列出所有模块或模块内所有脚本", "[module]") { arg ->
            if (!checkPermission(this)) return@ICommand sendMessage("[red]你没有权限使用该命令".with())
            if (arg.isEmpty()) {
                val list = manager.loadedInitScripts.values.map {
                    "[purple]{name} [blue]{desc}\n".with("name" to it.clsName.padEnd(20), "desc" to it.name)
                }
                return@ICommand sendMessage(
                    """
                    [yellow]==== [light_yellow]已加载模块[yellow] ====
                    {list}
                """.trimIndent().with("list" to list)
                )
            }
            val module = arg[0].let(::getScript)?.let { it as? IInitScript }
                ?: return@ICommand sendMessage("[red]找不到模块".with())
            val list = module.children.map {
                "[purple]{name} [blue]{desc}\n".with("name" to it.clsName.padEnd(20), "desc" to it.name)
            }
            sendMessage(
                """
                [yellow]==== [light_yellow]{module}脚本[yellow] ====
                {list}
            """.trimIndent().with("module" to module.name, "list" to list)
            )
        })
        addSub(ICommand(null, "reload", "重载一个脚本或者模块", "<module[/script]>") { arg ->
            if (!checkPermission(this)) return@ICommand sendMessage("[red]你没有权限使用该命令".with())
            val success: Boolean = when (val script = arg.getOrNull(0)?.let(::getScript)) {
                is IInitScript -> manager.reloadInit(script) != null
                is IContentScript -> manager.reloadContent(script.module!!, script) != null
                else -> return@ICommand sendMessage("[red]找不到模块或者脚本".with())
            }
            sendMessage((if (success) "[green]重载成功" else "[red]加载失败").with())
        })
        addSub(ICommand(null, "loadScript", "加载一个新脚本或者模块", "<filePath>", listOf("load")) { arg ->
            if (!checkPermission(this)) return@ICommand sendMessage("[red]你没有权限使用该命令".with())
            val file = arg.getOrNull(0)?.let(Config.rootDir::resolve)
                ?: return@ICommand sendMessage("[red]未找到对应文件".with())
            val success: Boolean = when {
                file.name.endsWith(Config.moduleDefineSuffix) -> manager.loadModule(file) != null
                file.name.endsWith(Config.contentScriptSuffix) -> {
                    val module = getScript(arg[0].split('/')[0]) as? IInitScript
                        ?: return@ICommand sendMessage("[red]找不到模块,请确定模块已先加载".with())
                    manager.loadContent(module, file) != null
                }
                else -> return@ICommand sendMessage("[red]不支持的文件格式".with())
            }
            sendMessage((if (success) "[green]加载脚本成功" else "[red]加载失败,查看后台以了解详情").with())
        })
    }

    private fun getScript(name: String): IBaseScript? {
        if (name.contains('/')) {
            val split = name.split('/', limit = 2)
            val module = (getScript(split[0]) as? IInitScript) ?: return null
            return module.children.find { it.clsName.equals(split[1], true) }
        } else {
            return manager.loadedInitScripts[name.toLowerCase()]
        }
    }
}