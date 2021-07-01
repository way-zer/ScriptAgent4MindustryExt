package coreMindustry.lib

import cf.wayzer.scriptAgent.define.ISubScript
import cf.wayzer.scriptAgent.define.ScriptDsl
import coreLibrary.lib.CommandContext
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import mindustry.gen.Player

@ScriptDsl
fun ISubScript.command(name: String, description: String, init: CommandInfo.() -> Unit) {
    RootCommands += CommandInfo(this, name, description, init)
}

@Deprecated("use new command api", ReplaceWith("command(name,description){init\nbody(handler)}"))
fun ISubScript.command(name: String, description: String, init: CommandInfo.() -> Unit, handler: CommandHandler) {
    RootCommands += CommandInfo(this, name, description) {
        init()
        body(handler)
    }
}

//常见拼写错误，但不报错
@Suppress("unused")
@Deprecated("请检查变量是否使用正确, Vars.player 为null", ReplaceWith("error(\"服务器中不允许使用该变量\")"), DeprecationLevel.ERROR)
val ISubScript.player: Player
    get() = error("服务器中不允许使用该变量")