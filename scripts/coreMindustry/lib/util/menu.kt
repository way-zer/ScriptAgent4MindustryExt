package coreMindustry.lib.util

import coreLibrary.lib.CommandContext
import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.util.calPage
import coreLibrary.lib.util.menu
import coreMindustry.lib.MsgType
import coreMindustry.lib.player
import coreMindustry.lib.reply
import coreMindustry.lib.sendMessage
import mindustry.gen.Player

fun <E> Player?.sendMenuPhone(title: String, list: List<E>, page: Int?, prePage: Int, handle: (E) -> PlaceHoldString) {
    CommandContext().also {
        it.player = this
        it.reply = { m -> sendMessage(m) }
    }.sendMenuPhone(title, list, page, prePage, handle)
}

fun <E> CommandContext.sendMenuPhone(
    title: String,
    list: List<E>,
    page: Int?,
    prePage: Int,
    handle: (E) -> PlaceHoldString
) {
    fun getPage(i: Int) = menu(title, list, i, prePage, handle)
    if (this.player?.con?.mobile == true && page == null) {
        val total = calPage(1, prePage, list.size).second
        (total downTo 1).forEach {
            reply(getPage(it), MsgType.InfoMessage)
        }
    } else
        reply(getPage(page ?: 1))
}