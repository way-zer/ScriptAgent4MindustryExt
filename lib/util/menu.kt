package coreLibrary.lib.util

import coreLibrary.lib.PlaceHoldString
import coreLibrary.lib.with
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