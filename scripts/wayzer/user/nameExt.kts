package wayzer.user

import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.TypeBinder

@Savable(serializable = false)
val realName = mutableMapOf<String, String>()
customLoad(::realName) { realName.putAll(it) }

val TypeBinder<*>.tree: Map<String, Any> by reflectDelegate()

registerVarForType<Player>().apply {
    registerChild("prefix", "名字前缀,可通过prefix.xxx变量注册") { p ->
        PlaceHoldApi.typeBinder<Player>().run {
            val keys = tree.keys.filter { it.startsWith("prefix.") }.sorted()
            keys.joinToString("") { k ->
                resolve(this@registerChild, p, k)?.let { resolveVarForString(it) }.orEmpty()
            }
        }
    }
    registerChild("suffix", "名字后缀,可通过suffix.xxx变量注册") { p ->
        PlaceHoldApi.typeBinder<Player>().run {
            val keys = tree.keys.filter { it.startsWith("suffix.") }.sorted()
            keys.joinToString("") { k ->
                resolve(this@registerChild, p, k)?.let { resolveVarForString(it) }.orEmpty()
            }
        }
    }
}


fun Player.updateName() {
    name = "[white]{player.prefix}[]{name}[white]{player.suffix}".with(
        "player" to this,
        "name" to (realName[uuid()] ?: "NotInit")
    ).toString()
}

listen<EventType.PlayerConnect> {
    val p = it.player
    realName[p.uuid()] = p.name
    p.updateName()
}
onEnable {
    loop(Dispatchers.game) {
        Groups.player.forEach {
            if (it.uuid() !in realName)
                realName[it.uuid()] = it.name
        }
        delay(5000)
        Groups.player.forEach { it.updateName() }
    }
}