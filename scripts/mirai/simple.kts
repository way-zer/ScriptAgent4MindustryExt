package mirai

import net.mamoe.mirai.message.data.MessageSource.Key.quote

globalEventChannel().subscribeGroupMessages {
    contains("hello", true).reply {
        message.quote() + "你好" + At(sender)
    }
    content { message.filterIsInstance<PlainText>().any { it.content.contains("欢迎新人") } }
        .reply { message.firstIsInstanceOrNull<At>()?.let { PlainText("欢迎") + it } ?: "欢迎新人!" }
}