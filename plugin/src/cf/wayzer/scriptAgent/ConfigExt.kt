@file:Suppress("UnusedReceiverParameter")

package cf.wayzer.scriptAgent

import arc.util.CommandHandler
import cf.wayzer.scriptAgent.util.DSLBuilder

var Config.version by DSLBuilder.lateInit<String>()
    internal set
var Config.mainScript by DSLBuilder.lateInit<String>()
    internal set
var Config.clientCommands by DSLBuilder.lateInit<CommandHandler>()
    internal set
var Config.serverCommands by DSLBuilder.lateInit<CommandHandler>()
    internal set