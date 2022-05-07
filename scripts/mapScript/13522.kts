package mapScript

import arc.util.Align
import coreLibrary.lib.util.loop
import mindustry.game.Team
/**@author Lucky_Clover
 * ClearUp: WayZer */
onEnable {
    val initState = Team.all.associateWith { it.rules().buildSpeedMultiplier }
    val totalCore = state.teams.getActive().sum { it.cores.size }

    val techSpeed = 0.002f
    val techAverage = 1 + techSpeed * totalCore / state.teams.active.count()

    loop(Dispatchers.game) {
        delay(5000)
        var infotext = "[red]科[yellow]技[green]战[blue]争[white]。\n[violet]规则[white]：核心数量越多科技越快，提高队伍全局属性\n"
        infotext += state.teams.getActive().joinToString("\n") { team ->
            val teamTechSpeed = (1 + techSpeed * team.cores.size)
            team.team.rules().blockHealthMultiplier *= teamTechSpeed / techAverage
            team.team.rules().blockDamageMultiplier *= teamTechSpeed / techAverage
            team.team.rules().unitDamageMultiplier *= teamTechSpeed / techAverage

            team.team.rules().buildSpeedMultiplier *= teamTechSpeed
            team.team.rules().unitBuildSpeedMultiplier *= teamTechSpeed

            "[#${team.team.color}]${team.team.name}[white]队科技属性提升：[red]${team.team.rules().blockHealthMultiplier / initState[team.team]!!}"
        }
        Call.infoPopup(
            infotext, 2.013f,
            Align.topLeft, 350, 0, 0, 0
        )
    }
}
