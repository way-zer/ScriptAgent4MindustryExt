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

object RootCommands : Commands() {
    override fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        if (name == "help") return //RootCommands don't need help
        super.addSub(name, command, isAliases)
        fun CommandHandler.register(){
            removeCommand(name)
            register(name, "[arg...]", command.description) { arg, player: Player? ->
                command(CommandContext().apply{
                    reply = {reply(it, MsgType.Message)}
                    this.player = player
                    thisCommand = command
                    prefix = "/$name"
                    this.arg = arg.getOrNull(0)?.split(' ')?: emptyList()
                })
            }
        }
        if(command.type.server())
            Config.serverCommands.register()
        if(command.type.client())
            Config.clientCommands.register()
    }

    override fun removeSub(name: String) {
        subCommands[name]?.let {
            if(it.type.server())
                Config.serverCommands.removeCommand(name)
            if(it.type.client())
                Config.clientCommands.removeCommand(name)
        }
        super.removeSub(name)
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

    init {
        RootCommands::class.java.getContextModule()!!.apply {
            listenTo<PermissionRequestEvent> {
                if (context.player?.isAdmin != false)
                    result = true
            }
        }
    }
}

// TODO 覆盖原版接口
class MyCommandHandler(private var prefix: String) : CommandHandler("") {
    override fun setPrefix(prefix: String) {
        this.prefix = prefix
    }
    override fun <T : Any?> register(text: String, params: String, description: String, runner: CommandRunner<T>): Command {
        if (text != "help")
            RootCommands += CommandInfo(null, text, description, { usage = params }) {
                @Suppress("UNCHECKED_CAST")
                runner.accept(arg.toTypedArray(), player as T)
            }
        return Command(text, "", description, runner)
    }

    override fun <T : Any?> register(text: String, description: String, runner: CommandRunner<T>): Command = register(text, "", description, runner)
    override fun removeCommand(text: String) {
        RootCommands.removeSub(text)
    }

    override fun handleMessage(message: String?, params: Any?): CommandResponse {
        if (message?.startsWith(prefix) != true) return CommandResponse(ResponseType.noCommand, null, null)
        RootCommands.invoke(CommandContext().apply {
            player = params as? Player
            thisCommand = CommandInfo(null, "", "", {}, RootCommands)
            reply = { reply(it, MsgType.Message) }
            prefix = "/"
            this.arg = message.removePrefix(prefix).split(' ')
        })
        return CommandResponse(ResponseType.valid, null, message)
    }
}

enum class CommandType {
    Client, Server, Both;

    fun client() = this == Client || this == Both
    fun server() = this == Server || this == Both
}
var CommandInfo.type by DSLBuilder.dataKeyWithDefault { CommandType.Both }
/**
 * null for console or other
 */
var CommandContext.player by DSLBuilder.dataKey<Player>()
fun CommandContext.reply(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f) {
    if (player == null) {
        println(ColorApi.handle("$text[RESET]", ColorApi::consoleColorHandler))
    } else {
        player.sendMessage("{msg}".with("msg" to text, "player" to player!!), type, time)
    }
}