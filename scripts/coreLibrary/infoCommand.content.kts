package coreLibrary

import coreLibrary.lib.ConfigBuilder.Companion.configs
import coreLibrary.lib.PlaceHold.registeredVars

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "info", "获取一个脚本的具体信息") {
            usage = "<module[/script]>"
            permission = "scriptAgent.info"
            onComplete {
                onComplete(0) {
                    (arg[0].split('/')[0].let(ScriptManager::getScript)?.let { it as IInitScript }?.children
                        ?: ScriptManager.loadedInitScripts.values).map { it.id }
                }
            }
            body {
                if (arg.isEmpty()) replyUsage()
                val script = ScriptManager.getScript(arg[0]) ?: returnReply("[red]找不到脚本,请确定加载成功,并输入正确".with())

                val configs = script.configs.map {
                    "[purple]{key} [blue]{desc}\n".with("key" to it.path, "desc" to (it.desc.firstOrNull() ?: ""))
                }
                val registeredVars = script.registeredVars.map {
                    "[purple]{key} [blue]{desc}\n".with("key" to it.key, "desc" to it.value)
                }

                returnReply(
                    """
                [yellow]==== [light_yellow]{name}信息[yellow] ====
                [cyan]配置项:
                {configs}
                [cyan]提供的变量:
                {registeredVars}
                [cyan]注册的指令:暂未实现
            """.trimIndent().with("name" to script.clsName, "configs" to configs, "registeredVars" to registeredVars)
                )
            }
        })
        onDisable { removeAll(thisRef) }
    }
}