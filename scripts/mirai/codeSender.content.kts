package mirai

subscribeMessages {
    case("绑定") {
        if (this is GroupMessageEvent) {
            reply("绑定请私聊我")
            return@case
        }
        val qq = sender.id
        val generate = depends("wayzer/user/profileBind")?.import<(Long) -> Int>("generate")
        if (generate == null) {
            reply("绑定服务暂不可用，请联系管理")
            return@case
        }
        reply(
            """
            你好${sender.nick}
            复制'/bind ${generate(qq).toString().padStart(6, '0')}'到游戏中，即可完成绑定。
        """.trimIndent()
        )
    }
}