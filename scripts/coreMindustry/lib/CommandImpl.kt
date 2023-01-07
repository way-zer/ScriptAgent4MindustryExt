@file:Suppress("unused")

package coreMindustry.lib

import arc.struct.Seq
import arc.util.CommandHandler
import cf.wayzer.scriptAgent.*
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mindustry.gen.Player

object RootCommands : Commands() {
    override fun getSubCommands(context: CommandContext?): Map<String, CommandInfo> {
        if (context == null) return super.getSubCommands(null)
        //合并原版指令
        val origin = (if (context.player != null) Config.clientCommands else Config.serverCommands)
            .let { originHandler ->
                originHandler.commandList.associate {
                    it.text.lowercase() to CommandInfo(null, it.text, it.description) {
                        usage = it.paramText
                        body {
                            (if (originHandler is MyCommandHandler) originHandler.origin else originHandler).handleMessage(
                                originHandler.prefix + it.text + " " + arg.joinToString(" "),
                                player
                            )
                        }
                    }
                }
            }
        return origin + subCommands.filterValues { if (context.player != null) it.type.client() else it.type.server() }
    }

    override fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        if (command.type.server())
            Config.serverCommands.removeCommand(name)
        if (command.type.client())
            Config.clientCommands.removeCommand(name)
        return super.addSub(name, command, isAliases)
    }

    init {
        arrayOf(Config.clientCommands, Config.serverCommands).forEach {
            it.removeCommand("help")
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

    suspend fun tabComplete(player: Player?, args: List<String>): List<String> {
        var result: List<String> = emptyList()
        try {
            onComplete(CommandContext().apply {
                this.player = player
                reply = {}
                replyTabComplete = { result = it;CommandInfo.Return() }
                arg = args
            })
        } catch (_: CommandInfo.Return) {
        }
        return result
    }

    /**
     * @param text 输入字符串，应当经过trimInput处理
     * @param player 控制台为null
     * @param prefix 指令前缀,例如'/'
     */
    suspend fun handleInput(text: String, player: Player?, prefix: String = "") {
        if (text.isEmpty()) return
        withContext(Dispatchers.game) {
            RootCommands.invoke(CommandContext().apply {
                this.player = player
                hasPermission = {
                    player == null || player.admin || player.hasPermission(it)
                }
                reply = { player.sendMessage(it, MsgType.Message) }
                this.prefix = prefix.ifEmpty { "* " }
                this.arg = text.removePrefix(prefix).split(' ')
            })
        }
    }
}

class MyCommandHandler(prefix: String, val origin: CommandHandler) : CommandHandler(prefix) {
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
        thisContextScript().launch(Dispatchers.game) {
            RootCommands.handleInput(raw, params as Player?, prefix)
        }
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
    player?.sendMessage(text, type, time) ?: reply(text)
}