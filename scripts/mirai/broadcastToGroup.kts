val groupId by config.key(0L, "广播的群号,0代表不启用")

registerVar("mirai.broadcast", "在群内广播消息", fun(msg: String) {
    if (groupId <= 0) return
    Bot.instancesSequence.forEach {
        launch {
            val group = it.getGroup(groupId)
            group?.sendMessage(msg)
        }
    }
})