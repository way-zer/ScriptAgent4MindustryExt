@file:Depends("coreLibrary/lang", "多语言支持-核心")
@file:Depends("coreLibrary/extApi/KVStore", "储存语言设置")

package wayzer.user

import cf.wayzer.placehold.DynamicVar
import org.h2.mvstore.type.StringDataType

name = "玩家语言设置"

val settings = contextScript<coreLibrary.extApi.KVStore>().open("langSettings", StringDataType.INSTANCE)

var PlayerData.lang: String
    get() = settings[id] ?: player?.locale ?: "zh"
    set(v) {
        if (lang == v) return
        if (v == player?.locale) {
            settings.remove(id)
        } else {
            settings[id] = v
        }
    }

registerVarForType<Player>()
    .registerChild("lang", "多语言支持",  {
        kotlin.runCatching { PlayerData[it].lang }.getOrNull()
    })

command("lang", "设置语言") {
    permission = "wayzer.lang.set"
    type = CommandType.Client
    body {
        if (arg.isEmpty()) returnReply("[yellow]你的当前语言是: {receiver.lang}".with())
        val data = PlayerData[player!!]
        data.lang = arg[0]
        reply("[green]你的语言已设为 {v}".with("v" to data.lang))
    }
}

PermissionApi.registerDefault("wayzer.lang.set")