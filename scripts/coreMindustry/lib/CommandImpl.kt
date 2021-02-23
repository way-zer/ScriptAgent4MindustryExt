@file:Suppress("unused")

package coreMindustry.lib

import arc.struct.Seq
import arc.util.CommandHandler
import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.getContextModule
import cf.wayzer.scriptAgent.listenTo
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.*
import coreLibrary.lib.event.PermissionRequestEvent
import coreMindustry.lib.util.sendMenuPhone
import mindustry.gen.Player

object RootCommands : Commands() {
    private var overwrite = true
    override fun getSubCommands(context: CommandContext?): Map<String, CommandInfo> {
        if (!overwrite || context == null) return super.getSubCommands(context)
        val origin =
            (if (context.player != null) Config.clientCommands else Config.serverCommands).let { originHandler ->
                originHandler.commandList.associate {
                    it.text.toLowerCase() to CommandInfo(null, it.text, it.description) {
                        usage = it.paramText
                        body {
                            prefix = prefix.removePrefix("* ")
                            (if (originHandler is MyCommandHandler) originHandler.origin else originHandler).handleMessage(
                                prefix + arg.joinToString(" "),
                                player
                            )
                        }
                    }
                }
            }
        return origin + subCommands.filterValues { if (context.player != null) it.type.client() else it.type.server() }
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
        try {
            onComplete(CommandContext().apply {
                this.player = player
                reply = {}
                replyTabComplete = { result = it;CommandInfo.Return() }
                arg = args
            })
        } catch (e: CommandInfo.Return) {
        }
        return result
    }

    override fun onHelp(context: CommandContext, explicit: Boolean) {
        if (!explicit) return context.reply("[red]无效指令,请使用/help查询".with())
        assert(overwrite)
        val showDetail = context.arg.firstOrNull() == "-v"
        val page = context.arg.lastOrNull()?.toIntOrNull()

        context.sendMenuPhone("帮助", getSubCommands(context).values.toSet().filter {
            (it.permission.isBlank() || context.hasPermission(it.permission))
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
                if (context.player?.admin == true)
                    result = true
            }
        }
    }

    fun trimInput(text: String) = buildString {
        var start = 0
        var end = text.length - 1
        while (start < text.length && text[start] == ' ') start++
        while (end >= 0 && text[end] == ' ') end--
        var lastBlank = false
        for (i in start..end) {
            val nowBlank = text[i] == ' '
            if (!lastBlank || !nowBlank)
                append(text[i])
            lastBlank = nowBlank
        }
    }

    /**
     * @param text 输入字符串，应当经过trimInput处理
     * @param player 控制台为null
     * @param prefix 指令前缀,例如'/'
     */
    fun handleInput(text: String, player: Player?, prefix: String = "") {
        if (text.isEmpty()) return
        RootCommands.invoke(CommandContext().apply {
            this.player = player
            if (player == null)
                hasPermission = { true }
            reply = { reply(it, MsgType.Message) }
            this.prefix = if (prefix.isEmpty()) "* " else prefix
            this.arg = text.removePrefix(prefix).split(' ')
        })
    }
}

class MyCommandHandler(private var prefix0: String, val origin: CommandHandler) : CommandHandler("") {
    override fun setPrefix(prefix: String) {
        prefix0 = prefix
    }

    override fun getPrefix(): String = prefix0
    override fun <T : Any?> register(
        text: String,
        params: String,
        description: String,
        runner: CommandRunner<T>
    ): Command {
        return origin.register(text, params, description, runner)
    }

    override fun <T : Any?> register(text: String, description: String, runner: CommandRunner<T>): Command =
        register(text, "", description, runner)

    override fun removeCommand(text: String) {
        return origin.removeCommand(text)
    }

    override fun getCommandList(): Seq<Command> {
        return origin.commandList
    }

    override fun handleMessage(raw: String?, params: Any?): CommandResponse {
        val message = raw?.let(RootCommands::trimInput)
        if (message?.startsWith(prefix) != true || message.isEmpty())
            return CommandResponse(ResponseType.noCommand, null, null)
        assert(params is Player?)
        RootCommands.handleInput(raw, params as Player?, prefix)
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
    player.sendMessage(text, type, time)
}