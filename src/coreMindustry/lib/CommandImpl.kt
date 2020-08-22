@file:Suppress("unused")

package coreMindustry.lib

import arc.struct.Array
import arc.util.CommandHandler
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.*
import coreLibrary.lib.event.PermissionRequestEvent
import coreMindustry.lib.util.sendMenuPhone
import mindustry.entities.type.Player

object RootCommands : Commands() {
    var overwrite = true
    override fun getSub(context: CommandContext): coreLibrary.lib.CommandHandler? {
        if (!overwrite) return super.getSub(context)
        val origin = (if (context.player != null) Config.clientCommands else Config.serverCommands).let { originHandler ->
            originHandler.commandList.associate {
                it.text.toLowerCase() to CommandInfo(null, it.text, it.description, { usage = it.paramText }) {
                    (if (originHandler is MyCommandHandler) originHandler.origin else originHandler).handleMessage(prefix + arg.joinToString(" "), player)
                }
            }
        }
        return context.checkArg(0, origin + subCommands) { it.toLowerCase() }
    }

    override fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        if (overwrite) {
            if (command.type.server())
                Config.serverCommands.removeCommand(name)
            if (command.type.client())
                Config.clientCommands.removeCommand(name)
            return super.addSub(name, command, isAliases)
        }
        super.addSub(name, command, isAliases)
        fun CommandHandler.register() {
            removeCommand(name)
            register(name, "[arg...]", command.description) { arg, player: Player? ->
                command(CommandContext().apply {
                    reply = { reply(it, MsgType.Message) }
                    this.player = player
                    thisCommand = command
                    prefix = "/$name"
                    this.arg = arg.getOrNull(0)?.split(' ') ?: emptyList()
                })
            }
        }
        if (command.type.server())
            Config.serverCommands.register()
        if (command.type.client())
            Config.clientCommands.register()
    }

    override fun removeSub(name: String) {
        if (overwrite) return super.removeSub(name)
        subCommands[name]?.let {
            if (it.type.server())
                Config.serverCommands.removeCommand(name)
            if (it.type.client())
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

    override fun onHelp(context: CommandContext, explicit: Boolean) {
        if (!explicit) return context.reply("[red]无效指令,请使用/help查询".with())
        assert(overwrite)
        val showDetail = context.arg.firstOrNull() == "-v"
        val page = context.arg.lastOrNull()?.toIntOrNull()

        val origin = (if (context.player != null) Config.clientCommands else Config.serverCommands).commandList
                .map { CommandInfo(null, it.text, it.description, { usage = it.paramText }) {} }
        context.sendMenuPhone("帮助", (subCommands.values.toSet() + origin).filter {
            (if (context.player != null) it.type.client() else it.type.server())
                    && (it.permission.isBlank() || context.hasPermission(it.permission))
        }, page, 10) {
            context.helpInfo(it, showDetail)
        }
    }

    init {
        if (overwrite) arrayOf(Config.clientCommands, Config.serverCommands).forEach {
            it.removeCommand("help")
        }
        RootCommands::class.java.getContextModule()!!.apply {
            listenTo<PermissionRequestEvent> {
                if (context.player?.isAdmin != false)
                    result = true
            }
        }
    }
}

// TODO 覆盖原版接口
class MyCommandHandler(private var prefix: String, val origin: CommandHandler) : CommandHandler("") {
    override fun setPrefix(prefix: String) {
        this.prefix = prefix
    }

    override fun <T : Any?> register(text: String, params: String, description: String, runner: CommandRunner<T>): Command {
        return origin.register(text, params, description, runner)
    }

    override fun <T : Any?> register(text: String, description: String, runner: CommandRunner<T>): Command = register(text, "", description, runner)
    override fun removeCommand(text: String) {
        return origin.removeCommand(text)
    }

    override fun getCommandList(): Array<Command> {
        return origin.commandList
    }

    override fun handleMessage(message: String?, params: Any?): CommandResponse {
        if (message?.startsWith(prefix) != true) return CommandResponse(ResponseType.noCommand, null, null)
        RootCommands.invoke(CommandContext().apply {
            player = params as? Player
            thisCommand = CommandInfo(null, "", "", {}, RootCommands)
            reply = { reply(it, MsgType.Message) }
            prefix = this@MyCommandHandler.prefix.let { if (it.isEmpty()) "* " else it }
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