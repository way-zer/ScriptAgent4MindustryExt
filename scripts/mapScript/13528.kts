package mapScript

import arc.util.Align
import coreLibrary.lib.util.loop

/**@author Lucky_Clover WayZer */
/** function: introduce tech in non-pvp mode*/
name = "科技战争 生存版"
onEnable {
    val techSpeed = state.rules.tags.getFloat("@techSpeed", 0.0001f)

    val initBlockHealth = state.rules.defaultTeam.rules().blockHealthMultiplier
    val initBlockDamage = state.rules.defaultTeam.rules().blockDamageMultiplier
    val initUnitDamage = state.rules.defaultTeam.rules().unitDamageMultiplier
    val initBuildSpeed = state.rules.defaultTeam.rules().buildSpeedMultiplier
    val initUnitSpeed = state.rules.defaultTeam.rules().unitBuildSpeedMultiplier
    val initBuildCost = state.rules.buildCostMultiplier

    var techcount = 1f

    loop(Dispatchers.game) {
        delay(5000)

        val techIncreased = techSpeed * state.rules.defaultTeam.cores().size

        techcount += techIncreased

        state.rules.defaultTeam.rules().apply {
            blockHealthMultiplier = initBlockHealth * techcount
            blockDamageMultiplier = initBlockDamage * techcount
            unitDamageMultiplier = initUnitDamage * techcount

            buildSpeedMultiplier = initBuildSpeed * techcount
            unitBuildSpeedMultiplier = initUnitSpeed * techcount
        }

        state.rules.apply {
            buildCostMultiplier = initBuildCost / techcount
        }
        Call.setRules(state.rules)

        val infotext = """
            [red]科[yellow]技[green]模[blue]式[white]
            [violet]规则[white]：核心数量越多科技越快，提高队伍全局属性
            队伍全局属性提升：[red]${"%.2f".format(techcount * 100)}%[]
            科研速度：[red]+${"%.2f".format(techIncreased * 100)}%[]
            """.trimIndent()

        Call.infoPopup(
            infotext, 5.013f,
            Align.topLeft, 350, 0, 0, 0
        )
    }
}
