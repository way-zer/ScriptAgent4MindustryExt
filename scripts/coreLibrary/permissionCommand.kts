package coreLibrary

import coreLibrary.lib.event.RequestPermissionEvent

val handler = PermissionApi.StringPermissionHandler()
onEnable { PermissionApi.handlers.addFirst(handler) }
onDisable { PermissionApi.handlers.remove(handler) }

var groups by config.key(
    "groups", mapOf("@default" to emptyList<String>()),
    "权限设置", "值为权限，@开头为组,支持末尾通配符.*"
) {
    handler.clear()
    it.forEach { (g, list) ->
        handler.registerPermission(g, list)
    }
}

Commands.controlCommand += CommandInfo(this, "permission", "权限系统配置") {
    aliases = listOf("pm")
    usage = "<group> <add/list/remove/delGroup> [permission]"
    onComplete {
        onComplete(0) { PermissionApi.allKnownGroup.toList() }
        onComplete(1) { listOf("add", "list", "remove", "delGroup") }
    }
    body {
        if (arg.isEmpty()) returnReply("当前已有组: {list}".with("list" to PermissionApi.allKnownGroup))
        val group = arg[0]
        when (arg.getOrNull(1)?.lowercase() ?: "") {
            "add" -> {
                if (arg.size < 3) returnReply("[red]请输入需要增减的权限".with())
                val now = groups[group].orEmpty()
                if (arg[2] !in now)
                    groups = groups + (group to (now + arg[2]))
                returnReply(
                    "[green]{op}权限{permission}到组{group}".with(
                        "op" to "添加", "permission" to arg[2], "group" to group
                    )
                )
            }

            "remove" -> {
                if (arg.size < 3) returnReply("[red]请输入需要增减的权限".with())
                if (group in groups) {
                    val newList = groups[group].orEmpty() - arg[2]
                    groups = if (newList.isEmpty()) groups - group else groups + (group to newList)
                }
                returnReply(
                    "[green]{op}权限{permission}到组{group}".with(
                        "op" to "移除", "permission" to arg[2], "group" to group
                    )
                )
            }

            "", "list" -> {
                val now = groups[group].orEmpty()
                val defaults = PermissionApi.default.groups[group]?.allNodes().orEmpty()
                reply(
                    """
                        [green]组{group}当前拥有权限:[]
                        {list}
                        [green]默认定义权限:[]
                        {defaults}
                        [yellow]默认组权限仅可通过添加负权限修改
                    """.trimIndent().with(
                        "group" to group, "list" to now.toString(), "defaults" to defaults.toString()
                    )
                )
            }

            "delGroup".lowercase() -> {
                val now = groups[group].orEmpty()
                if (group in groups)
                    groups = groups - group
                returnReply(
                    "[yellow]移除权限组{group},原来含有:{list}".with(
                        "group" to group, "list" to now.toString()
                    )
                )
            }

            else -> replyUsage()
        }
    }
}
Commands.controlCommand.autoRemove(this)

val debug by config.key(false, "调试输出,如果开启,则会在后台打印权限请求")
listenTo<RequestPermissionEvent>(Event.Priority.Watch) {
    if (debug)
        logger.info("$permission $directReturn -- $group")
}