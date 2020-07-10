package coreLibrary

import cf.wayzer.script_agent.ScriptManager
import coreLibrary.lib.ConfigBuilder.Companion.configs
import coreLibrary.lib.PlaceHold.registeredVars

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "info", "获取一个脚本的具体信息", {usage="<module[/script]>"}) {
            if (!hasPermission("scriptAgent.info")) return@CommandInfo reply("[red]你没有权限使用该命令".with())
            if (arg.isEmpty()) return@CommandInfo reply("[red]请输入脚本".with())
            val script = ScriptManager.getScript(arg[0]) ?: return@CommandInfo reply("[red]找不到脚本,请确定加载成功,并输入正确".with())

            val configs = script.configs.map {
                "[purple]{key} [blue]{desc}\n".with("key" to it.path, "desc" to (it.desc.firstOrNull() ?: ""))
            }
            val registeredVars = script.registeredVars.map {
                "[purple]{key} [blue]{desc}\n".with("key" to it.key, "desc" to it.value)
            }

            reply("""
                [yellow]==== [light_yellow]{name}信息[yellow] ====
                [cyan]配置项:
                {configs}
                [cyan]提供的变量:
                {registeredVars}
                [cyan]注册的指令:暂未实现
            """.trimIndent().with("name" to script.clsName, "configs" to configs, "registeredVars" to registeredVars))
        })
        onDisable { removeAll(thisRef) }
    }
}