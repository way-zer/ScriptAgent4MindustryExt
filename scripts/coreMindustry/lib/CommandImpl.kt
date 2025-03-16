@file:Suppress("unused")

package coreMindustry.lib

import arc.struct.Seq
import arc.util.CommandHandler
import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.clientCommands
import cf.wayzer.scriptAgent.serverCommands
import cf.wayzer.scriptAgent.thisContextScript
import coreLibrary.lib.CommandContext
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.Commands
import coreLibrary.lib.PlaceHoldString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mindustry.gen.Player

object RootCommands {
    init {
        arrayOf(Config.clientCommands, Config.serverCommands).forEach {
            it.removeCommand("help")
        }
        Commands.Root.subCommandOverwrite = { parent ->
            val serverCommands = Config.serverCommands.let { cmds ->
                cmds.commandList.associate {
                    it.text.lowercase() to CommandInfo(null, it.text, it.description) {
                        usage = it.paramText
                        attr(NotForClient)
                        body {
                            cmds.handleMessage(cmds.prefix + it.text + " " + arg.joinToString(" "), null)
                        }
                    }
                }
            }
            val clientCommands = Config.clientCommands.let { cmds ->
                cmds.commandList.associate {
                    it.text.lowercase() to CommandInfo(null, it.text, it.description) {
                        usage = it.paramText
                        attr(ClientOnly)
                        body {
                            cmds.handleMessage(cmds.prefix + it.text + " " + arg.joinToString(" "), player)
                        }
                    }
                }
            }
            clientCommands + serverCommands + parent
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
            Commands.Root.onComplete(CommandContext().apply {
                receiver = player ?: CommandContext.ConsoleReceiver
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
            CommandContext().apply {
                receiver = player ?: CommandContext.ConsoleReceiver
                hasPermission = {
                    player == null || player.admin || player.hasPermission(it)
                }
                reply = { player.sendMessage(it, MsgType.Message) }
                this.prefix = prefix.ifEmpty { "* " }
                this.arg = text.removePrefix(prefix).split(' ')
                Commands.Root.handle()
            }
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

@Deprecated("use CommandAttr")
var CommandInfo.type: CommandType
    get() = throw NotImplementedError("use CommandAttr")
    set(value) {
        if (value == CommandType.Client) attr(ClientOnly)
        else if (value == CommandType.Server) attr(NotForClient)
    }

data object ClientOnly : Commands.Hidden {
    context(CommandContext) override suspend fun visible(): Boolean = receiver is Player
}

data object NotForClient : Commands.Hidden {
    context(CommandContext) override suspend fun visible(): Boolean = receiver !is Player
}

/**
 * null for console or other
 */
val CommandContext.player
    get() = receiver as? Player

fun CommandContext.reply(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f) {
    player?.sendMessage(text, type, time) ?: reply(text)
}