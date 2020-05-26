import kotlinx.coroutines.launch

val groupId by config.key(0L, "广播的群号,0代表不启用")

registerVar("mirai.broadcast", "在群内广播消息", fun(msg: String) {
    if (groupId <= 0) return
    launch {
        Bot.forEachInstance {
            val group = it.getGroup(groupId)
            group.sendMessage(msg)
        }
    }
})