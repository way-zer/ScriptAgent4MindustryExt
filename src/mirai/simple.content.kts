package mirai

botListen<GroupMessage> {
    if (message.contentToString().contains("hello", true)) {
        quoteReply(buildMessageChain {
            +"你好"
            +At(sender)
        })
    }
}
botListen<GroupMessage> {
    if (message.getOrNull(PlainText.Key)?.stringValue?.contains("欢迎新人") == true) {
        val at = message.getOrNull(At.Key)
        if (at == null) reply("欢迎新人!")
        else reply("欢迎".toMessage() + at)
    }
}