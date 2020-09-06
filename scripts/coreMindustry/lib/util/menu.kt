package coreMindustry.lib.util

import coreLibrary.lib.CommandContext
import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.util.calPage
import coreLibrary.lib.util.menu
import coreMindustry.lib.MsgType
import coreMindustry.lib.player
import coreMindustry.lib.sendMessage
import mindustry.entities.type.Player

fun <E> Player?.sendMenuPhone(title: String, list: List<E>, page: Int?, prePage: Int, handle: (E) -> PlaceHoldString) {
    fun getPage(i: Int) = menu(title, list, i, prePage, handle)
    if (this?.isMobile != true)
        return sendMessage(getPage(page ?: 1))
    if (page == null) {
        val total = calPage(1, prePage, list.size).second
        (total downTo 1).forEach {
            sendMessage(getPage(it), MsgType.InfoMessage)
        }
    } else
        sendMessage(getPage(page), MsgType.InfoMessage)
}

fun <E> CommandContext.sendMenuPhone(title: String, list: List<E>, page: Int?, prePage: Int, handle: (E) -> PlaceHoldString) {
    fun getPage(i: Int) = menu(title, list, i, prePage, handle)
    if (this.player?.isMobile != true)
        return reply(getPage(page ?: 1))
    if (page == null) {
        val total = calPage(1, prePage, list.size).second
        (total downTo 1).forEach {
            player!!.sendMessage(getPage(it), MsgType.InfoMessage)
        }
    } else
        player!!.sendMessage(getPage(page), MsgType.InfoMessage)
}