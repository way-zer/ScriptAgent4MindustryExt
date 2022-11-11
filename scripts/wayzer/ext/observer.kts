@file:Depends("wayzer/map/betterTeam")

package wayzer.ext

val teams = contextScript<wayzer.map.BetterTeam>()

command("ob", "切换为观察者") {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
    body {
        if (player!!.team() == teams.spectateTeam) {
            teams.teams.remove(player!!.uuid())
            teams.changeTeam(player!!)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]重新投胎到{player.team.colorizeName}"
                    .with("player" to player!!), type = MsgType.InfoToast, quite = true
            )
        } else {
            teams.changeTeam(player!!, teams.spectateTeam)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]选择成为观察者"
                    .with("player" to player!!), type = MsgType.InfoToast, quite = true
            )
            player!!.sendMessage("[green]再次输入指令可以重新投胎")
        }
    }
}

PermissionApi.registerDefault("wayzer.ext.observer")