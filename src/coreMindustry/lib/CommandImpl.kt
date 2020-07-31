@file:Suppress("unused")

package coreMindustry.lib

import arc.util.CommandHandler
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.*
import coreLibrary.lib.event.PermissionRequestEvent
import mindustry.entities.type.Player

class RootCommands(private val mindustryHandler: CommandHandler) : Commands() {
    override fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        if (name == "help") return //RootCommands don't need help
        super.addSub(name, command, isAliases)
        removeSub(name)
        mindustryHandler.register(name, "[arg...]", command.description) { arg, player: Player? ->
            command(CommandContext().apply{
                reply = {reply(it, MsgType.Message)}
                this.player = player
                thisCommand = command
                prefix = "/$name"
                this.arg = arg.getOrNull(0)?.split(' ')?: emptyList()
            })
        }
    }

    override fun removeSub(name: String) {
        super.removeSub(name)
        mindustryHandler.removeCommand(name)
    }
    fun tabComplete(player: Player?, args: List<String>): List<String> {
        var result: List<String> = emptyList()
        invoke(CommandContext().apply {
            this.player = player
            replyTabComplete = { result = it;CommandInfo.Return() }
            arg = args
        })
        return result
    }
    companion object{
        init {
            RootCommands::class.java.getContextModule()!!.apply {
                listenTo<PermissionRequestEvent> {
                    if(context.player?.isAdmin!=false)
                        result = true
                }
            }
        }
    }
}

/**
 * null for console or other
 */
var CommandContext.player by DSLBuilder.dataKey<Player>()
fun CommandContext.reply(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f){
    if (player == null) {
        println(ColorApi.handle("$text[RESET]", ColorApi::consoleColorHandler))
    } else {
        player.sendMessage("{msg}".with("msg" to text, "player" to player!!), type, time)
    }
}
val clientRootCommands = RootCommands(Config.clientCommands)
val serverRootCommands = RootCommands(Config.serverCommands)