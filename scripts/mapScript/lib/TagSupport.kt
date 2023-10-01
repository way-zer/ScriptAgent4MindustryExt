package mapScript.lib

import cf.wayzer.scriptAgent.define.Script
import mindustry.Vars
import mindustry.game.Rules
import kotlin.properties.ReadOnlyProperty

/**用于注册Tag类的mapScript，通常存放位置为`mapScript/tag/xxx` */
object TagSupport {
    // tag -> scriptId
    val knownTags = mutableMapOf<String, String>()

    fun findTags(rules: Rules): Map<String, String> {
        val mapTags = rules.tags.keys().toSet()
        return knownTags.filterKeys { it in mapTags }
    }
}

fun Script.registerMapTag(name: String) {
    TagSupport.knownTags[name] = id
}

fun Script.mapTag(name: String): ReadOnlyProperty<Any?, String> {
    registerMapTag(name)
    return ReadOnlyProperty { _, _ ->
        Vars.state.rules.tags.get(name).orEmpty()
    }
}