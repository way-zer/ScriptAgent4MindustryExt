package mirai

subscribeFriendMessages {
    case("绑定") {
        val qq = sender.id
        val generate = depends("wayzer/user/profileBind")?.import<(Long) -> Int>("generate")
        if (generate == null) {
            reply("绑定服务暂不可用，请联系管理")
            return@case
        }
        reply("你好${sender.nick}\n" +
                "复制'/bind ${generate(qq).toString().padStart(6, '0')}'到游戏中，即可完成绑定。")
    }
}
subscribeTempMessages{
    case("绑定") {
        val qq = sender.id
        val generate = depends("wayzer/user/profileBind")?.import<(Long) -> Int>("generate")
        if (generate == null) {
            reply("绑定服务暂不可用，请联系管理")
            return@case
        }
        reply("你好${sender.nameCard}\n" +
                "复制'/bind ${generate(qq).toString().padStart(6, '0')}'到游戏中，即可完成绑定。")
    }
}