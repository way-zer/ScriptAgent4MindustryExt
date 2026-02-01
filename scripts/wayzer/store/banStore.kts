@file:Depends("wayzer/user/ban")
@file:Implement(wayzer.user.PlayerBanStore::class)

import coreLib.db.DBApi
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.user.PlayerBan

object Table : IntIdTable("PlayerBanV2") {
    val ids = text("ids", eagerLoading = true)
    val reason = text("reason", eagerLoading = true)
    val operator = text("operator").nullable()
    val createTime = timestamp("createTime").defaultExpression(CurrentTimestamp)
    val endTime = timestamp("endTime").defaultExpression(CurrentTimestamp)
}
DBApi.registerTable(Table)

fun ResultRow.toBan() = PlayerBan(
    get(Table.id).value,
    ids = get(Table.ids).split("$").toSet(),
    reason = get(Table.reason),
    operator = get(Table.operator),
    createTime = get(Table.createTime),
    endTime = get(Table.endTime)
)

fun getByRowId(id: Int) = transaction {
    Table.selectAll().where { Table.id eq id }.firstOrNull()?.toBan()
}

/*override*/ fun create(
    ids: Set<String>,
    duration: java.time.Duration,
    reason: String,
    operator: String?
): PlayerBan =
    transaction {
        Table.insertAndGetId {
            it[Table.ids] = ids.joinToString("$", "$", "$")
            it[Table.endTime] = java.time.Instant.now() + duration
            it[Table.operator] = operator
            it[Table.reason] = reason
        }.let {
            getByRowId(it.value)!!
        }
    }

/*override*/ fun findNotEnd(id: String): PlayerBan? = transaction {
    Table.selectAll().where { (Table.ids like "%$${id}$%") and Table.endTime.greater(CurrentTimestamp) }
        .firstOrNull()?.toBan()
}

/*override*/ fun delete(record: Int): PlayerBan? = transaction {
    getByRowId(record)?.also {
        Table.deleteWhere { id eq record }
    }
}