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
        PlayerData[uuid()].secureProfile(this)?.apply {
            launch(Dispatchers.IO) {
                transaction {
                    lang = v
                }
            }
        } ?: let {
            tempLang[uuid()] = v
            sendMessage("[yellow]当前未绑定账号,语言设置将在退出游戏后重置".with())
        }
    }

registerVarForType<Player>()
    .registerChild("lang", "多语言支持", DynamicVar.obj { it.lang })

command("lang", "设置语言") {
    permission = "wayzer.lang.set"
    type = CommandType.Client
    body {
        if (arg.isEmpty()) returnReply("[yellow]你的当前语言是: {receiver.lang}".with())
        player!!.lang = arg[0]
        reply("[green]你的语言已设为 {v}".with("v" to player!!.lang))
    }
}
