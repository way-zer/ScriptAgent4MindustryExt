package wayzer.lib

import kotlin.math.ceil
import kotlin.math.min

fun <T> List<T>.page(page: Int, prePage: Int): List<T> {
    val totalPage = ceil(size / prePage.toDouble()).toInt()
    val newPage = when {
        page < 1 -> 1
        page > totalPage -> totalPage
        else -> page
    }
    return subList(prePage * (newPage - 1), min(size, prePage * newPage))
}