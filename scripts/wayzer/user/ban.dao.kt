package wayzer.user

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.PlayerData
import java.time.Duration
import java.time.Instant

data class PlayerBan(
    val id: Int,
    val ids: String,
    val reason: String,
    val operator: String?,
    val createTime: Instant,
    val endTime: Instant
) {
    constructor(row: ResultRow) : this(
        row[T.id].value,
        row[T.ids],
        row[T.reason],
        row[T.operator],
        row[T.createTime],
        row[T.endTime]
    )

    object T : IntIdTable("PlayerBanV2") {
        val ids = text("ids", eagerLoading = true)
        val reason = text("reason", eagerLoading = true)
        val operator = text("operator").nullable()
        val createTime = timestamp("createTime").defaultExpression(CurrentTimestamp)
        val endTime = timestamp("endTime").defaultExpression(CurrentTimestamp)
    }

    companion object {
        private fun getById(id: Int) = transaction {
            T.selectAll().where { T.id eq id }.firstOrNull()?.let { PlayerBan(it) }
        }

        fun create(ids: PlayerData, time: Duration, reason: String, operator: String?): PlayerBan = transaction {
            T.insertAndGetId {
                it[T.ids] = ids.idsInDB
                it[T.endTime] = Instant.now() + time
                it[T.operator] = operator
                it[T.reason] = reason
            }.let {
                getById(it.value)!!
            }
        }

        fun allNotEnd() = transaction {
            T.selectAll().where { T.endTime.greater(CurrentTimestamp) }
                .map { PlayerBan(it) }
        }

        fun findNotEnd(id: String): PlayerBan? = transaction {
            T.selectAll().where { (T.ids like "%$${id}$%") and T.endTime.greater(CurrentTimestamp) }.firstOrNull()
                ?.let { PlayerBan(it) }
        }

        fun delete(id: Int): PlayerBan? = transaction {
            getById(id)?.also {
                T.deleteWhere { T.id eq id }
            }
        }
    }
}