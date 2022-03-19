package wayzer.map

import coreLibrary.lib.event.RequestPermissionEvent

val group = "@mapRule"
fun whiteList(it: String) =
    it.startsWith("-wayzer.user.skills.")
            || it == "-wayzer.vote.skipwave"
            || it == "-wayzer.ext.gather"

@Savable(false)
var permissions: List<String> = emptyList()
listen<EventType.PlayEvent> {
    PermissionApi.default.unRegisterPermission(group, permissions)
    permissions = state.rules.tags.get("@permission")?.split(";").orEmpty()
        .filter { whiteList(it) }
    PermissionApi.default.registerPermission(group, permissions)
}

listenTo<RequestPermissionEvent>(Event.Priority.After) {
    if (permissions.isEmpty()) return@listenTo
    val index = group.indexOfLast { it.startsWith("@") }
    group = group.toMutableList().apply {
        add(index + 1, "@mapRule")
    }
}