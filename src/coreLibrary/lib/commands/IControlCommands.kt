package coreLibrary.lib.commands

import cf.wayzer.script_agent.IBaseScript
import cf.wayzer.script_agent.ScriptManager
import coreLibrary.lib.ICommands
import coreLibrary.lib.ISender

class IControlCommands : ICommands<ISender<*>>(null, "ScriptAgent", "ScriptAgent 控制指令", listOf("sa")) {
    val manager = ScriptManager
    fun getScript(name: String): IBaseScript? {
        return manager.getScript(name)
    }
}