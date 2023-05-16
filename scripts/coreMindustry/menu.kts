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
