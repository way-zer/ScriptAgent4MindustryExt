package wayzer

import cf.wayzer.placehold.PlaceHoldApi
import coreLibrary.lib.event.PermissionRequestEvent

name = "权限管理系统"

var groups by config.key(
    mapOf(
        "@default" to listOf("wayzer.ext.observer", "wayzer.ext.history", "wayzer.ext.gather", "wayzer.vote.*"),
        "@admin" to listOf(
            "wayzer.admin.ban", "wayzer.info.other", "wayzer.vote.ban",
            "wayzer.maps.host", "wayzer.maps.load", "wayzer.ext.team.change",
        ),
    ),
    "权限设置", "特殊组有:@default,@admin,@lvl0,@lvl1等,用户qq可单独做组", "值为权限，@开头为组,支持末尾通配符.*"
)

fun hasPermission(permission: String, list: List<String>): Boolean {
    list.forEach {
        when {
            it.startsWith("@") -> {
                if (checkGroup(permission, it))
                    return true
            }
            it.endsWith(".*") -> {
                if (permission.startsWith(it.removeSuffix("*")))
                    return true
            }
            else -> {
                if (permission == it) return true
            }
        }
    }
    return false
}

fun checkGroup(permission: String, groupName: String) =
    groups[groupName]?.let { hasPermission(permission, it) } ?: false

listenTo<PermissionRequestEvent> {
    if (context.player == null) return@listenTo//console
    try {
        fun check(body: () -> Boolean) {
            if (body()) {
                result = true
                CommandInfo.Return()
            }
        }
        check { checkGroup(permission, "@default") }
        val uuid = context.player!!.uuid()
        check { checkGroup(permission, uuid) }
        val profile = PlayerData[uuid].secureProfile(context.player!!) ?: return@listenTo
        check { checkGroup(permission, "qq${profile.qq}") }
        val level = (PlaceHoldApi.GlobalContext.typeResolve(profile, "level") ?: return@listenTo) as Int
        for (lvl in level downTo 0) {
            check { checkGroup(permission, "@lvl$lvl") }
        }
    } catch (e: CommandInfo.Return) {
    }
}

command("permission", "权限系统配置") {
    permission = "wayzer.permission"
    usage = "<group> <add/list/remove/delGroup> [permission]"
    onComplete {
        onComplete(0) { groups.keys.toList() }
        onComplete(1) { listOf("add", "list", "remove", "addGroup") }
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
                val now = groups[group].orEmpty()
                if (arg[2] in now)
                    groups = groups + (group to (now - arg[2]))
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
            "delgroup" -> {
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

command("madmin", "列出或添加删除管理") {
    this.usage = "[uuid/qq]"
    this.permission = "wayzer.permission.admin"
    body {
        val uuid = arg.getOrNull(0)
        if (uuid == null) {
            val list = groups.filter { it.value.contains("@admin") }.keys.joinToString()
            returnReply("Admins: {list}".with("list" to list))
        } else {
            val isQQ = uuid.length > 5 && uuid.toLongOrNull() != null
            val key = if (isQQ) "qq$uuid" else uuid
            val now = groups[key].orEmpty()
            if ("@admin" in now) {
                groups = if (now.size == 1) {
                    groups - key
                } else {
                    groups + (key to (now - "@admin"))
                }
                returnReply("[red]{uuid} [green] has been removed from Admins[]".with("uuid" to uuid))
            } else {
                if (isQQ) {
                    groups = groups + (key to (now + "@admin"))
                    reply("[green]QQ [red]{qq}[] has been added to Admins".with("qq" to uuid))
                } else {
                    val info = netServer.admins.getInfoOptional(uuid)
                        ?: returnReply("[red]Can't found player".with())
                    groups = groups + (key to (now + "@admin"))
                    reply("[green]Player [red]{info.name}({info.uuid})[] has been added to Admins".with("info" to info))
                }
            }
        }
    }
}