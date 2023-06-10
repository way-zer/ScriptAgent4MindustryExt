package coreMindustry

data class MenuChooseEvent(
    val player: Player, val menuId: Int, val value: Int
) : Event, Event.Cancellable {
    override var cancelled: Boolean = false

    companion object : Event.Handler()
}

listen<EventType.MenuOptionChooseEvent> {
    MenuChooseEvent(it.player, it.menuId, it.option).launchEmit(coroutineContext) { e ->
        if (!e.cancelled && it.menuId < 0)
            Call.hideFollowUpMenu(e.player.con, e.menuId)
    }
}

onEnable {
    val bak = Commands.defaultHelpImpl
    onDisable { Commands.defaultHelpImpl = bak }
    Commands.defaultHelpImpl = impl@{ context, explicit ->
        val player = context.player ?: return@impl bak(context, explicit)
        if (context.arg.isNotEmpty() && !explicit) return@impl context.reply("[red]无效指令,请使用/help查询".with())
        val showDetail = context.checkArg("-v")
        if (showDetail && !context.hasPermission("command.detail"))
            return@impl context.reply("[red]必须拥有command.detail权限才能查看完整help".with())

        val commands = getSubCommands(context).values.toSet().filter {
            showDetail || it.permission.isBlank() || context.hasPermission(it.permission)
        }
        PagedMenuBuilder(commands, selectedPage = context.arg.firstOrNull()?.toIntOrNull() ?: 1) { command ->
            option(buildString {
                append("[gold]${context.prefix}${command.name}")
                if (command.aliases.isNotEmpty())
                    append("[scarlet](${command.aliases.joinToString()})")
                appendLine(" [white]${command.usage}")
                append("[cyan]${command.description.toPlayer(player)}")
                if (showDetail) {
                    command.script?.let { append(" | ${it.id}") }
                    command.permission.takeUnless { it == "" }?.let { append(" | $it") }
                }
            }) {
                context.arg = listOf(command.name)
                context.reply("[yellow][快捷输入指令][] {command}".with("command" to (context.prefix + command.name)))
                invoke(context)
            }
        }.apply {
            msg = "点击选项将直接执行指令"
        }.sendTo(player, 60_000)
    }
}