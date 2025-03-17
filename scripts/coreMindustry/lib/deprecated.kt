package coreMindustry.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.command
import coreLibrary.lib.with
import mindustry.game.EventType
import mindustry.gen.Player

/**
 * 注册指令
 * 所有body将在 Dispatchers.game下调用, 费时操作请注意launch并切换Dispatcher
 */
@ScriptDsl
@Deprecated(
    "move to coreLibrary", ReplaceWith("command(name,description.with()){init()}", "coreLibrary.lib.command"),
    DeprecationLevel.HIDDEN
)
fun Script.command(name: String, description: String, init: CommandInfo.() -> Unit) {
    command(name, description.with()) { init() }
}

@Deprecated(
    "use new command api", ReplaceWith("command(name,description.with()){init\nbody(handler)}"), DeprecationLevel.ERROR
)
fun Script.command(name: String, description: String, init: CommandInfo.() -> Unit, handler: CommandHandler) {
    command(name, description.with()) {
        init()
        body(handler)
    }
}

/**
 * Support for utilContentOverwrite
 * auto re[init] when [EventType.ContentInitEvent]
 */
@ScriptDsl
@Deprecated("no use ContentsLoader", ReplaceWith("lazy{ init() }"), DeprecationLevel.HIDDEN)
inline fun <T : Any> Script.useContents(crossinline init: () -> T) = lazy { init() }

@Deprecated("use CommandAttr")
var CommandInfo.type: CommandType
    get() = throw NotImplementedError("use CommandAttr")
    set(value) {
        if (value == CommandType.Client) attr(ClientOnly)
        else if (value == CommandType.Server) attr(NotForClient)
    }


@Deprecated(
    "use PlaceHoldString", ReplaceWith("sendMessage(text.with(), type, time)", "coreLibrary.lib.with"),
    DeprecationLevel.ERROR
)
fun Player?.sendMessage(text: String, type: MsgType = MsgType.Message, time: Float = 10f) =
    sendMessage(text.with(), type, time)