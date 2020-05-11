package coreLibrary.lib.commands

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IBaseScript
import coreLibrary.lib.ICommands
import coreLibrary.lib.ISender

class IControlCommands : ICommands<ISender<*>>(null, "ScriptAgent", "ScriptAgent 控制指令", listOf("sa")) {
    val manager = Config.inst
    fun getScript(name: String): IBaseScript? {
        return manager.getScript(name)
    }
}