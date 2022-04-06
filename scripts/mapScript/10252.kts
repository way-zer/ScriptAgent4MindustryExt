package mapScript

import arc.util.Align
/**@author WayZer*/
name = "Boss挑战"
onEnable {
    launch(Dispatchers.game) {
        while (true) {
            delay(2000)
            val boss = state.boss() ?: continue
            Call.infoPopup(
                "[violet]Boss护盾值: [orange]${boss.shield}", 2.013f,
                Align.topLeft, 350, 0, 0, 0
            )
        }
    }
}