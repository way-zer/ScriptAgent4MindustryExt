package mapScript

import arc.util.Align
import coreLibrary.lib.util.loop
import mindustry.gen.Groups

/**@author Lucky_Clover WayZer */
name = "科技战争"
onEnable {
    val techSpeed = state.rules.tags.getFloat("@techSpeed", 0.002f)

    loop(Dispatchers.game) {
        delay(5000)

        val teams = Groups.player.mapTo(mutableSetOf()) { it.team() }
        teams.removeAll { !it.active() }
        val averageCore = teams.sumOf { it.cores().size }.toFloat() / teams.size

        var infotext = "[red]科[yellow]技[green]战[blue]争[white]。\n[violet]规则[white]：核心数量越多科技越快，提高队伍全局属性\n"
        infotext += teams.joinToString("\n") { team ->
            val teamTechSpeed = 1 + techSpeed * (team.cores().size / averageCore - 1)
            val delta = team.rules().blockHealthMultiplier * (teamTechSpeed - 1)
            team.rules().apply {
                blockHealthMultiplier *= teamTechSpeed
                blockDamageMultiplier *= teamTechSpeed
                unitDamageMultiplier *= teamTechSpeed

                buildSpeedMultiplier *= teamTechSpeed
                unitBuildSpeedMultiplier *= teamTechSpeed
            }

            val rate = team.rules().blockHealthMultiplier
            "[#${team.color}]${team.name}[white]队科技属性提升：[sky]${"%.3f".format(rate)} " +
                    "${if (delta >= 0) "[green]+" else "[red]"}${"%.4f".format(delta)}"
        }
        Call.infoPopup(
            infotext, 5.013f,
            Align.topLeft, 350, 0, 0, 0
        )
    }
}
