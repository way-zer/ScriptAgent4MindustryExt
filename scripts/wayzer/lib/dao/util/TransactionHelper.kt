package wayzer.lib.dao.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object TransactionHelper {
    private val threadLocal = ThreadLocal<MutableList<Transaction.() -> Unit>>()

    fun begin() = threadLocal.set(mutableListOf())
    fun lateUpdate(body: Transaction.() -> Unit) = TransactionManager.currentOrNull()?.body()
        ?: threadLocal.get()?.add(body) ?: error("Not in TransactionHelper scope")

    fun end(): Transaction.() -> Unit {
        val list = threadLocal.get()
        threadLocal.remove()
        return {
            list.forEach { it() }
        }
    }

    inline fun withScope(body: () -> Unit): Transaction.() -> Unit {
        begin()
        body()
        return end()
    }

    inline fun withAsyncFlush(scope: CoroutineScope, body: () -> Unit): Job {
        begin()
        body()
        val flusher = end()
        return scope.launch(Dispatchers.IO) {
            transaction { flusher() }
        }
    }

    fun flushAsync(scope: CoroutineScope): Job {
        val flusher = end()
        return scope.launch(Dispatchers.IO) {
            transaction { flusher() }
        }
    }
}