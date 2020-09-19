package mirai

subscribeGroupMessages {
    contains("hello", true) {
        quoteReply(buildMessageChain {
            +"你好"
            +At(sender)
        })
    }
    content { message.filterIsInstance<PlainText>().any { it.content.contains("欢迎新人") } }
            .reply { message[At.Key]?.let { PlainText("欢迎") + it } ?: "欢迎新人!" }
}