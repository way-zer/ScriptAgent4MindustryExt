package cf.wayzer

import arc.util.CommandHandler
import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.util.DSLBuilder

object ConfigExt {
    var Config.clientCommands by DSLBuilder.dataKey<CommandHandler>()
    var Config.serverCommands by DSLBuilder.dataKey<CommandHandler>()
}