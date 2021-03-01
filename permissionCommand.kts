package coreLibrary

var groups by config.key(
    "groups", mapOf("@default" to emptyList<String>()),
    "权限设置", "值为权限，@开头为组,支持末尾通配符.*"
) {
    Handler.importConfig(it)
}

object Handler : PermissionApi.StringPermissionHandler() {
    fun importConfig(groups: Map<String, List<String>>) {
        clear()
        groups.forEach { (g, list) ->
            registerPermission(g, list)
        }
    }
}

Commands.controlCommand += CommandInfo(this, "permission", "权限系统配置") {
    usage = "<group> <add/list/remove/delGroup> [permission]"
    onComplete {
        onComplete(0) { groups.keys.toList() }
        onComplete(1) { listOf("add", "list", "remove", "delGroup") }
    }
    body {
        if (arg.isEmpty()) returnReply("当前已有组: {list}".with("list" to groups.keys))
        if (arg.size < 2) replyUsage()
        val group = arg[0]
        when (arg[1].toLowerCase()) {
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
            "list" -> {
                val now = groups[group].orEmpty()
                returnReply(
                    "[green]组{group}当前拥有权限:[]\n{list}".with(
                        "group" to group, "list" to now.toString()
                    )
                )
            }
            "delGroup".toLowerCase() -> {
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