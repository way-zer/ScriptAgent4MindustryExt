@file:Depends("coreLibrary/lang", "多语言支持-核心")

package wayzer.ext

import cf.wayzer.placehold.DynamicVar
import org.jetbrains.exposed.sql.transactions.transaction

name = "玩家语言设置"

val tempLang = mutableMapOf<String, String>()//uuid -> lang
listen<EventType.PlayerLeave> {
    tempLang.remove(it.player.uuid())
}

var Player.lang: String
    get() = tempLang[uuid()]
        ?: PlayerData[uuid()].profile?.lang
        ?: locale
    set(v) {
        if (lang == v) return
        launch(Dispatchers.game) {
            setLang(this@lang, v)
        }
    }

suspend fun setLang(player: Player, v: String) {
    PlayerData[player.uuid()].secureProfile(player)?.apply {
        withContext(Dispatchers.IO) {
            transaction {
                lang = v
            }
        }
    } ?: let {
        tempLang[player.uuid()] = v
        player.sendMessage("[yellow]当前未绑定账号,语言设置将在退出游戏后重置".with())
    }
}

registerVarForType<Player>()
    .registerChild("lang", "多语言支持", DynamicVar.obj {
        kotlin.runCatching { it.lang }.getOrNull()
    })

command("lang", "设置语言") {
    permission = "wayzer.lang.set"
    type = CommandType.Client
    body {
        if (arg.isEmpty()) returnReply("[yellow]你的当前语言是: {receiver.lang}".with())
        setLang(player!!, arg[0])
        reply("[green]你的语言已设为 {v}".with("v" to player!!.lang))
    }
}

PermissionApi.registerDefault("wayzer.lang.set")