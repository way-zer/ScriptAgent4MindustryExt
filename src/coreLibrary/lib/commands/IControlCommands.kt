package coreLibrary.lib.commands

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IBaseScript
import cf.wayzer.script_agent.IInitScript
import coreLibrary.lib.ICommands
import coreLibrary.lib.ISender

class IControlCommands : ICommands<ISender<*>>(null, "ScriptAgent", "ScriptAgent 控制指令", listOf("sa")) {
    val manager = Config.inst
    fun getScript(name: String): IBaseScript? {
        if (name.contains('/')) {
            val split = name.split('/', limit = 2)
            val module = (getScript(split[0]) as? IInitScript) ?: return null
            return module.children.find { it.clsName.equals(split[1], true) }
        } else {
            return manager.loadedInitScripts[name.toLowerCase()]
        }
    }
}