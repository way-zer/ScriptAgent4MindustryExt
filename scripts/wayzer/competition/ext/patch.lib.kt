package wayzer.competition.ext

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import mindustry.content.Planets
import mindustry.game.Rules
import mindustry.type.Planet

/*
 * 在地图加载时调用一次body
 * 应保证所有副作用操作在地图加载前执行，在新地图加载前回收
 */
data class MapPatch (val name : String, val desc : String,
                     val env : Collection<Planet>,
                     val condition : Rules.() -> Boolean,
                     val body : () -> Unit) {
    operator fun invoke() = body()
}

object PatchManager {
    internal val patches = mutableListOf<MapPatch>()
    fun add(p: MapPatch) = patches.add(p)
    fun remove(p: MapPatch) = patches.remove(p)
    fun randomOrNull(rules: Rules) : MapPatch? = patches.filter { it.env.contains(rules.planet) && it.condition(rules) }.randomOrNull()
    fun findOrNull(name: String) : MapPatch? = patches.find { it.name == name }
}

@ScriptDsl
fun Script.mapPatch(name: String, desc: String, env: Collection<Planet> = any, cond: Rules.() -> Boolean = {true}, body: () -> Unit) {
    onEnable {
        PatchManager.add(MapPatch(name, desc, env, cond, body))
    }
}

internal val any: Collection<Planet>
    get() = setOf(Planets.serpulo, Planets.erekir, Planets.sun)

