package mirai

import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.firstIsInstanceOrNull

globalEventChannel().subscribeGroupMessages {
    contains("hello", true).reply {
        QuoteReply(message) + "你好" + At(sender)
    }
}

globalEventChannel().subscribeGroupMessages {
    content { message.filterIsInstance<PlainText>().any { it.content.contains("欢迎新人") } }
        .reply { message.firstIsInstanceOrNull<At>()?.let { PlainText("欢迎") + it } ?: "欢迎新人!" }
}


