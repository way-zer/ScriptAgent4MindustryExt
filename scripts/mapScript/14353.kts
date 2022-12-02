//核心单位修改支持
@file:Depends("coreMindustry/utilMapRule", "修改核心单位,单位属性")

//初音未来地图专用脚本

import mindustry.content.UnitTypes
import mindustry.world.blocks.storage.CoreBlock
import mindustry.content.Blocks

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
        broadcast("初音の地图脚本已加载".with())
    }
}
