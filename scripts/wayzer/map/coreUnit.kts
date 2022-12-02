package main

import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import kotlinx.coroutines.Dispatchers
import mindustry.ctype.ContentType
import mindustry.type.UnitType
import mindustry.type.unit.MissileUnitType
import mindustry.type.unit.NeoplasmUnitType
import mindustry.Vars
import mindustry.Vars.content
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.core.ContentLoader
import mindustry.ctype.Content
import mindustry.ctype.MappableContent
import mindustry.game.EventType
import mindustry.world.blocks.storage.CoreBlock
import java.lang.reflect.Modifier
import kotlin.reflect.KMutableProperty0

val bakMap = mutableMapOf<KMutableProperty0<*>, Any?>()

/**Should invoke in [Dispatchers.game] */
fun <T : Content, R : T> newContent(origin: T, block: (origin: T) -> R): R {
    val bak = content
    content = object : ContentLoader() {
        override fun transformName(name: String?) = bak?.transformName(name) ?: name
        override fun handleContent(content: Content?) = Unit
        override fun handleMappableContent(content: MappableContent?) = Unit
    }
    return try {
        block(origin).also { new ->
            origin::class.java.fields.forEach {
                if (!it.declaringClass.isInstance(new)) return@forEach
                if (Modifier.isPublic(it.modifiers) && !Modifier.isFinal(it.modifiers)) {
                    it.set(new, it.get(origin))
                }
            }
        }
    } finally {
        content = bak
    }
}

fun <T> registerMapRule(field: KMutableProperty0<T>, checkRef: Boolean = true, valueFactory: (T) -> T) {
    synchronized(bakMap) {
        @Suppress("UNCHECKED_CAST")
        val old = (bakMap[field] as T?) ?: field.get()
        val new = valueFactory(old)
        if (field !in bakMap && checkRef && new is Any && new === old)
            error("valueFactory can't return the same instance for $field")
        field.set(new)
        bakMap[field] = old
    }
}

fun reset(){
    synchronized(bakMap) {
        bakMap.forEach { (field, bakValue) ->
            @Suppress("UNCHECKED_CAST")
            (field as KMutableProperty0<Any?>).set(bakValue)
        }
        bakMap.clear()
    }
}

listen<EventType.ResetEvent> {
    reset()
}
onDisable{
    reset()
}

fun changeCoreUnit(unitType: UnitType?) {
    launch(Dispatchers.game){
        val list = content.getBy<UnitType>(ContentType.unit)
        var coreUnit: UnitType
        if (unitType != null)
            coreUnit = unitType
        else
            coreUnit = list.find { type -> type.toString() == Vars.state.rules.tags.get("@coreUnit","") } ?: return@launch

        if(!coreUnit.playerControllable || coreUnit == UnitTypes.block) {
            broadcast("[red]修改单位不合法,核心单位修改无效".with())
            return@launch
        }
        if (!coreUnit.flying && !coreUnit.canBoost && !coreUnit.allowLegStep && coreUnit !is NeoplasmUnitType){
            if (coreUnit.weapons.any { it.rotate }){
                broadcast("[yellow]修改核心单位无法在核心站立,自动设置为飞行".with())
                registerMapRule(coreUnit::flying) { true }
            } else{
                broadcast("[red]修改核心单位无法在核心站立且无武器可在飞行时正确射击,修改无效".with())
                return@launch
            }
        }
        if (coreUnit is MissileUnitType) {
            broadcast("[yellow]修改核心单位为导弹单位，自动设置为无限存在时间".with())
            registerMapRule(coreUnit::lifetime) {Float.MAX_VALUE}
        }
        registerMapRule((Blocks.coreShard as CoreBlock)::unitType) { coreUnit }
        registerMapRule((Blocks.coreFoundation as CoreBlock)::unitType) { coreUnit }
        registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { coreUnit }

        registerMapRule((Blocks.coreAcropolis as CoreBlock)::unitType) { coreUnit }
        registerMapRule((Blocks.coreBastion as CoreBlock)::unitType) { coreUnit }
        registerMapRule((Blocks.coreCitadel as CoreBlock)::unitType) { coreUnit }

        broadcast("[green]修改成功 核心单位变为{emoji}{type}{emoji}".with("emoji" to coreUnit.emoji(),"type" to coreUnit.toString()))
    }
}

listen<EventType.PlayEvent>{
    changeCoreUnit(null)
}

command("changeCoreUnit", "修改核心单位") {
    usage = "[类型ID=列出]"
    permission = "wayzer.admin.changeCoreUnit"
    aliases = listOf("CUc","修改核心单位")
    body {
        val list = content.getBy<UnitType>(ContentType.unit)
        val type = arg.getOrNull(0)?.toIntOrNull()?.let { list.items.getOrNull(it) } ?: returnReply(
            "[red]请输入类型ID: {list}"
                .with("list" to list.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString())
        )
        changeCoreUnit(type)
    }
}

command("resetCoreUnit", "重置核心单位") {
    permission = "wayzer.admin.changeCoreUnit"
    aliases = listOf("CUr","重置核心单位")
    body {
        reset()
        broadcast("[green]核心单位已经重置".with())
    }
}

