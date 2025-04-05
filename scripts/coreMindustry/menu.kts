package coreMindustry

import coreLibrary.lib.Commands.Hidden

data class MenuChooseEvent(
    val player: Player, val menuId: Int, val value: Int
) : Event, ReceivedEvent {
    override var received: Boolean = false

    companion object : Event.Handler()
}

listen<EventType.MenuOptionChooseEvent> {
    MenuChooseEvent(it.player, it.menuId, it.option).launchEmit(coroutineContext + Dispatchers.game) { e ->
        if (!e.received && it.menuId < 0)
            Call.hideFollowUpMenu(e.player.con, e.menuId)
    }
}

onEnable {
    val bak = Commands.helpOverwrite
    onDisable { Commands.helpOverwrite = bak }
    Commands.helpOverwrite = impl@{ cmds, showAll, page ->
        val player = player ?: return@impl

        var commands = cmds.subCommands().values.toSet().sortedBy { it.name }
        if (!showAll) commands = commands.filter { info ->
            info.attrs.all { it !is Hidden || it.visible() }
        }
        MenuV2(player) {
            title = if (prefix.isEmpty()) "Help" else "Help: $prefix"
            msg = "点击选项将直接执行指令"
            columnPreRow = 1
            renderPaged(commands, page) {
                option(buildString {
                    append("[gold]${prefix}${it.name}")
                    if (it.aliases.isNotEmpty())
                        append("[scarlet](${it.aliases.joinToString()})")
                    appendLine(" [white]${it.usage}")
                    append("[cyan]${it.description.toPlayer(player)}")
                    if (showAll) {
                        it.script?.let { append(" | ${it.id}") }
                        if (it.permission.isNotBlank()) append(" | ${it.permission}")
                    }
                }) {
                    arg = listOf(it.name)
                    reply("[yellow][快捷输入指令][] {command}".with("command" to (prefix + it.name)))
                    cmds.handle()
                }
            }
        }.send().awaitWithTimeout()
        CommandInfo.Return()
    }
}