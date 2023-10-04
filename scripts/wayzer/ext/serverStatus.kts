package wayzer.ext

command("status", "获取服务器信息") {
    aliases = listOf("服务器状态")
    body {
        reply(
            """
            |[green]服务器状态[]
            |   [green]地图: [ [yellow]{map.id} [green]][yellow]{map.name}[green] 模式: [yellow]{map.mode} [green]波数[yellow]{state.wave}
            |   [green]{tps} TPS, {heapUse} MB used, UPTIME {state.uptime}[]
            |   [green]总单位数: {state.allUnit} 玩家数: {state.playerSize}
            |   [yellow]被禁封总数: {state.allBan}
            """.trimMargin().with()
        )
    }
}