package wayzer.reGrief

listen<EventType.PlayerJoin> {
    val info = PlayerData[it.player]
    val existed = Groups.player.asIterable().find { player ->
        val otherInfo = PlayerData[player]
        info != otherInfo && otherInfo.ids.any { id -> id in info.ids }
    } ?: return@listen
    broadcast(
        "[yellow]{player.name}[] 可能为{existed.name}小号,请管理员注意观察".with(
            "player" to it.player, "existed" to existed
        )
    )
}
