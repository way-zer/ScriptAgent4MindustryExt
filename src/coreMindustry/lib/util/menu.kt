package coreMindustry.lib.util

import coreLibrary.lib.CommandContext
import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.with
import coreMindustry.lib.MsgType
import coreMindustry.lib.player
import coreMindustry.lib.sendMessage
import mindustry.entities.type.Player
import kotlin.math.ceil
import kotlin.math.min


fun calPage(page: Int, prePage: Int, size: Int): Pair<Int, Int> {
    val totalPage = ceil(size / prePage.toDouble()).toInt()
    val newPage = when {
        page < 1 -> 1
        page > totalPage -> totalPage
        else -> page
    }
    return newPage to totalPage
}

fun <E> menu(title: String, list: List<E>, page: Int, prePage: Int, handle: (E) -> PlaceHoldString): PlaceHoldString {
    val (newPage, totalPage) = calPage(page, prePage, list.size)
    val list2 = list.subList((newPage - 1) * prePage, min(list.size, newPage * prePage)).map(handle)
    return """
                [green]==== [white]{title}[green] ====
                {list}
                [green]==== [white]{page}/{total}[green] ====
            """.trimIndent().with("title" to title, "list" to list2, "page" to newPage, "total" to totalPage)
}

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