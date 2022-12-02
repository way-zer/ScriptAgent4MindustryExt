package mirai

import net.mamoe.mirai.message.data.MessageSource.Key.quote

globalEventChannel().subscribeMessages {
    case("绑定") {
        if (this is GroupMessageEvent) {
            subject.sendMessage(message.quote() + "绑定请私聊我")
            return@case
        }
        val qq = sender.id
        val generate = depends("wayzer/user/ext/profileBind")?.import<(Long) -> Int>("generate")
        if (generate == null) {
            subject.sendMessage("绑定服务暂不可用，请联系管理")
            return@case
        }
        subject.sendMessage(
            """
            你好${sender.nick}
            复制'/bind ${generate(qq).toString().padStart(6, '0')}'到游戏中，即可完成绑定。
        """.trimIndent()
        )
    }
}