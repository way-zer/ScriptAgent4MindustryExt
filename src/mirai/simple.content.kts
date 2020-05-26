package mirai

subscribeGroupMessages {
    contains("hello", true) {
        quoteReply(buildMessageChain {
            +"你好"
            +At(sender)
        })
    }
    content { message.filterIsInstance<PlainText>().any { it.stringValue.contains("欢迎新人") } }
            .reply { message.getOrNull(At.Key)?.let { "欢迎".toMessage() + it } ?: "欢迎新人!" }
}