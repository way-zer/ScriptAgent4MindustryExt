package cf.wayzer

import arc.util.CommandHandler
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptAgent
import cf.wayzer.script_agent.ScriptManager
import cf.wayzer.script_agent.util.DSLBuilder.Companion.dataKey
import mindustry.Vars
import mindustry.plugin.Plugin

class ScriptAgent4Mindustry: Plugin() {
    private var Config.clientCommands by dataKey<CommandHandler>()
    private var Config.serverCommands by dataKey<CommandHandler>()
    init {
        ScriptAgent.load()
    }

    override fun registerClientCommands(handler: CommandHandler) {
        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    override fun init() {
        ScriptManager().loadDir(Vars.dataDirectory.file())
    }
}