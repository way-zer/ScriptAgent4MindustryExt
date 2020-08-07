package coreStandalone.lib

import cf.wayzer.script_agent.IContentScript
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.Commands

object RootCommands : Commands()

fun IContentScript.command(
    name: String,
    description: String,
    init: CommandInfo.() -> Unit = {},
    handler: CommandHandler
) {
    RootCommands.addSub(CommandInfo(this, name, description, init, handler))
}