package coreMindustry.lib

import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.ColorApi
import coreLibrary.lib.ConsoleColor
import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.with
import mindustry.Vars.netServer
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player

object ContentHelper {
    fun logToConsole(text: String) {
        Log.info(ColorApi.handle(text, ColorApi::consoleColorHandler))
    }

    fun mindustryColorHandler(color: ColorApi.Color): String {
        if (color is ConsoleColor) {
            return when (color) {
                ConsoleColor.LIGHT_YELLOW -> "[gold]"
                ConsoleColor.LIGHT_PURPLE -> "[magenta]"
                ConsoleColor.LIGHT_RED -> "[scarlet]"
                ConsoleColor.LIGHT_CYAN -> "[cyan]"
                ConsoleColor.LIGHT_GREEN -> "[acid]"
                else -> "[${color.name}]"
            }
        }
        return ""
    }
}

enum class MsgType { Message, InfoMessage, InfoToast }

fun broadcast(
    text: PlaceHoldString,
    type: MsgType = MsgType.Message,
    time: Float = 10f,
    quite: Boolean = false,
    players: Iterable<Player> = Groups.player
) {
    if (!quite) ContentHelper.logToConsole(text.toString())
    MindustryDispatcher.runInMain {
        players.forEach {
            if (it.con != null)
                it.sendMessage(text, type, time)
        }
    }
}

fun Player?.sendMessage(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f) {
    if (this == null) ContentHelper.logToConsole(text.toString())
    else {
        if (con == null) return
        MindustryDispatcher.runInMain {
            val msg = ColorApi.handle(
                "{text}".with("text" to text, "player" to this, "receiver" to this).toString(),
                ContentHelper::mindustryColorHandler
            )
            when (type) {
                MsgType.Message -> Call.sendMessage(this.con, msg, null, null)
                MsgType.InfoMessage -> Call.infoMessage(this.con, msg)
                MsgType.InfoToast -> Call.infoToast(this.con, msg, time)
            }
        }
    }
}

fun Player?.sendMessage(text: String, type: MsgType = MsgType.Message, time: Float = 10f) =
    sendMessage(text.with(), type, time)

val Config.clientCommands by DSLBuilder.dataKeyWithDefault {
    netServer?.clientCommands ?: CommandHandler("/")
}
val Config.serverCommands by DSLBuilder.dataKeyWithDefault {
    println("serverCommands Not exists")
    CommandHandler("")
}