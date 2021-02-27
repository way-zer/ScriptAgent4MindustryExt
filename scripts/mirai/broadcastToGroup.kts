package mirai

val groupId by config.key(0L, "广播的群号,0代表不启用")

fun broadcast(msg: String) {
    if (groupId <= 0) return
    Bot.instancesSequence.forEach {
        launch {
            val group = it.getGroup(groupId)
            group?.sendMessage(msg)
        }
    }
}
export(::broadcast)