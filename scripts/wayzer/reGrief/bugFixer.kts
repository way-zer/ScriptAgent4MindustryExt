package wayzer.reGrief

import arc.struct.ObjectMap
import arc.util.pooling.Pools
import mindustry.core.Version
import mindustry.game.EventType.ResetEvent
import mindustry.net.Administration
import mindustry.world.blocks.storage.StorageBlock
import mindustry.world.blocks.storage.StorageBlock.StorageBuild

//update on 7/5
// 修复 Pool内存泄漏bug mod图卡服(无法进入)
// 历史修复 塑钢带bug(断开连接)

//内存泄漏
listen<ResetEvent> {
    Pools::class.java.getDeclaredField("typePools").apply {
        isAccessible = true
        (get(null) as ObjectMap<*, *>).clear()
    }
}

//未确认加入bug
registerActionFilter { it.player.con.hasConnected }

if (Version.build == 146) {
    //e仓库刷物品bug
    registerActionFilter {
        if (it.type != Administration.ActionType.placeBlock) return@registerActionFilter true
        val block = it.block as? StorageBlock ?: return@registerActionFilter true
        val old = it.tile.block() as? StorageBlock ?: return@registerActionFilter true
        (!block.coreMerge && old.coreMerge && (it.tile.build as StorageBuild).linkedCore != null).not()
    }
}