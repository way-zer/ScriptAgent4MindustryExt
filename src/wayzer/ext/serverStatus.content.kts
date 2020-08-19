package wayzer.ext

val format by config.key("""
    |[green]服务器状态[]
    |   [green]地图: [ [yellow]{map.id} [green]][yellow]{map.name}[green] 模式: [yellow]{map.mode}
    |   [green]{fps} FPS, {heapUse} MB used[]
    |   [green]总单位数: {state.allUnit} 玩家数: {state.playerSize}
    |   [yellow]被禁封总数: {state.allBan}
""".trimMargin())


command("status", "获取服务器信息") {
    reply(format.with())
}