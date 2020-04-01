package cf.wayzer

import arc.util.CommandHandler
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.util.DSLBuilder

object ConfigExt {
    var Config.clientCommands by DSLBuilder.dataKey<CommandHandler>()
    var Config.serverCommands by DSLBuilder.dataKey<CommandHandler>()
}