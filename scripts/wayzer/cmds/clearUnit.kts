package wayzer.cmds

command("clearUnit", "清除所有单位") {
    permission = dotId
    body {
        broadcast("[green]管理员使用了灭霸(清除所有单位)".with())
        Groups.unit.toList().forEach { it.kill() }
    }
}