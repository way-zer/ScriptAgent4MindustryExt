//核心单位修改支持
@file:Depends("coreMindustry/utilMapRule", "修改核心单位,单位属性")

//初音未来地图专用脚本

import arc.math.Mathf
import arc.util.Align
import arc.util.Time
import coreLibrary.lib.util.loop
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.entities.Units
import mindustry.game.Team
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Iconc.*
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.blocks.storage.CoreBlock
import kotlin.random.Random

listen<EventType.PlayEvent> {
    onEnable {
        contextScript<coreMindustry.UtilMapRule>().apply {
            //核心属性
            registerMapRule((Blocks.coreShard as CoreBlock)::unitType) { UnitTypes.mega }
            registerMapRule((Blocks.coreFoundation as CoreBlock)::unitType) { UnitTypes.mega }
            registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { UnitTypes.mega }

            registerMapRule((Blocks.coreBastion as CoreBlock)::unitType) { UnitTypes.mega }
            registerMapRule((Blocks.coreCitadel as CoreBlock)::unitType) { UnitTypes.mega }
            registerMapRule((Blocks.coreAcropolis as CoreBlock)::unitType) { UnitTypes.mega }

            registerMapRule(UnitTypes.mega::flying) { false }
        }
    }
}