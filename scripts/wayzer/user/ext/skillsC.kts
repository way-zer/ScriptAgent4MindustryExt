@file:Depends("wayzer/user/userService")
package wayzer.user.ext

import arc.util.io.Writes
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.gen.Building
import wayzer.user.ext.SkillsC.Api.skill
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Duration


import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi.with

import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.entities.Units
import mindustry.Vars
import mindustry.type.Item

import kotlin.math.min
import kotlin.math.max

import mindustry.world.Block;
import mindustry.game.Team

import mindustry.world.*;
import mindustry.world.blocks.environment.*;

@Savable(false)
val used = mutableMapOf<String, Long>()
customLoad(::used, used::putAll)
listen<EventType.ResetEvent> { used.clear() }

@Suppress("unused")
companion object Api {
    lateinit var script: SkillsC
    private val used get() = script.used

    @DslMarker
    annotation class SkillScopeMarker

    @Suppress("MemberVisibilityCanBePrivate")
    class SkillScope(val name: String, val player: Player, val ctx: CommandContext) {
        @SkillScopeMarker
        fun returnReply(msg: PlaceHoldString): Nothing = ctx.returnReply(msg)

        @SkillScopeMarker
        fun checkNotPvp() {
            if (state.rules.pvp)
               returnReply("[red]当前模式禁用".with())
        }

        @SkillScopeMarker
        fun checkWave(Waves: Int) {
            if (state.wave < Waves)
                returnReply("[red]波数不符合要求".with())
        }
        


        /** @param coolDown in ms,  -1一局冷却 */
        //$id
        @SkillScopeMarker
        fun checkOrSetCoolDown(coolDown: Int) {
            val id = PlayerData[player.uuid()].profile?.id?.value ?: 0
            val key = "${name}@$id"
            if (key in used) {
                if (coolDown == -1)
                    returnReply("[red]该技能每局限用一次".with())
                else if (used[key]!! >= System.currentTimeMillis())
                    returnReply("[red]技能冷却，还剩{time:秒}".with("time" to Duration.ofMillis(used[key]!! - System.currentTimeMillis())))
            }
            used[key] = System.currentTimeMillis() + coolDown
        }

        @SkillScopeMarker
        fun broadcastSkill(skill: String) = broadcast(
            "[gold][贡献者技能][green]{player.name}[white]使用了[green]{skill}[white]技能."
                .with("player" to player, "skill" to skill), quite = true, type = MsgType.InfoToast
        )
    }

    /**
     *
     */
    fun Script.skill(name: String, desc: String, vararg aliases: String, body: SkillScope.() -> Unit) {
        command(name, desc) {
            permission = "wayzer.user.skillsC.$name"
            this.aliases = aliases.toList()
            type = CommandType.Client
            body {
                @Suppress("MemberVisibilityCanBePrivate")
                if (player!!.dead())
                    returnReply("[red]你已死亡".with())
                SkillScope(name, player!!, this).body()
            }
        }
    }

    fun syncTile(vararg builds: Building) {
        val outStream = ByteArrayOutputStream()
        val write = DataOutputStream(outStream)
        builds.forEach {
            write.writeInt(it.pos())
            it.writeAll(Writes.get(write))
        }
        Call.blockSnapshot(builds.size.toShort(), outStream.toByteArray())
    }
}
Api.script = this




skill("latum", "技能: 召唤一个latum，只能存活60秒", "将死之虫") {
    checkOrSetCoolDown(-1)
    checkNotPvp()

    val unit = player.unit()
    
    broadcastSkill("将死之虫")
    
    UnitTypes.latum.create(player.team()).apply {
        set(unit.x, unit.y)
        add()
        maxHealth = maxHealth / 2
        launch(Dispatchers.game) {
       		 delay(60000L)
           kill()
       }
    }
}

skill("miner", "技能: 生成一个8x8的地雷阵，冷却300秒", "地雷阵") {
    checkOrSetCoolDown(300000)
    checkNotPvp()

    val unit = player.unit()

    for(x in -3..4){
        for(y in -3..4){

            val tile = world.tiles.get(        
                unit.tileX() + x,
                unit.tileY() + y
            )

            world.tiles.getc(unit.tileX() + x, unit.tileY() + y).apply {
                if (block() == Blocks.air){
                    tile?.setNet(Blocks.shockMine, player.team(), 0)
                }
            }
        
        }
    }
    
    broadcastSkill("地雷阵")
}

skill("fluid", "技能: 生成一块随机液体，冷却300秒", "大液比") {
    checkOrSetCoolDown(300000)
    checkNotPvp()

    val unit = player.unit()
    val tile = world.tiles.get(        
       unit.tileX(),
       unit.tileY()
    )

    world.tiles.getc(unit.tileX(), unit.tileY()).apply {
        if (block() != Blocks.air){
            returnReply("[red]这里已经有一个方块了".with())
        }
    } 
    var randomWater = (0..100).random()
    
    if(randomWater <= 20){
    	tile?.setFloorNet(Blocks.water)
    }else if(randomWater <= 40){
    	tile?.setFloorNet(Blocks.cryofluid)
    }else if(randomWater <= 60){
    	tile?.setFloorNet(Blocks.tar)
    }else if(randomWater <= 80){
    	tile?.setFloorNet(Blocks.slag)
    }else{
    	tile?.setFloorNet(Blocks.arkyciteFloor)
    }


    
    broadcastSkill("大液比")
}

skill("wallKiller", "技能:粉碎3x3的墙壁，冷却600秒", "墙壁粉碎者") {
    checkOrSetCoolDown(600000)
    checkNotPvp()

    val unit = player.unit()

    for(x in -1..1){
        for(y in -1..1){

            val tile = world.tiles.get(        
                unit.tileX() + x,
                unit.tileY() + y
            )

            world.tiles.getc(unit.tileX() + x, unit.tileY() + y).apply {
                if (tile?.breakable() == false){
                    tile?.setNet(Blocks.air)
                }
            }
        
        }
    }
    
    broadcastSkill("墙壁粉碎者")
}


skill("flying", "技能: 让当前单位飞起，冷却1500秒", "飞起") {
    checkNotPvp()
    checkOrSetCoolDown(1500000)
    val unit = player.unit()
    unit.elevation = 1f
    
    broadcastSkill("飞起")
}


