@file:Depends("wayzer/user/ban")
@file:Depends("coreLibrary/DBApi", "数据库储存")

package wayzer.user

import coreLibrary.DBApi.DB.registerTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.rmi.server.UnicastRemoteObject
import java.time.Duration
import java.time.Instant

object Table : IntIdTable("PlayerBanV2") {
    val ids = text("ids", eagerLoading = true)
    val reason = text("reason", eagerLoading = true)
    val operator = text("operator").nullable()
    val createTime = timestamp("createTime").defaultExpression(CurrentTimestamp)
    val endTime = timestamp("endTime").defaultExpression(CurrentTimestamp)
}
registerTable(Table)

object ServiceImpl : UnicastRemoteObject(), Ban.PlayerBanStore {
    private fun readResolve(): Any = ServiceImpl
    private fun ResultRow.toBan() = Ban.PlayerBan(
        get(Table.id).value,
        ids = get(Table.ids).split("$").toSet(),
        reason = get(Table.reason),
        operator = get(Table.operator),
        createTime = get(Table.createTime),
        endTime = get(Table.endTime)
    )

    private fun getById(id: Int) = transaction {
        Table.selectAll().where { Table.id eq id }.firstOrNull()?.toBan()
    }

    override fun create(ids: Set<String>, duration: Duration, reason: String, operator: String?): Ban.PlayerBan =
        transaction {
            Table.insertAndGetId {
                it[Table.ids] = ids.joinToString("$", "$", "$")
                it[Table.endTime] = Instant.now() + duration
                it[Table.operator] = operator
                it[Table.reason] = reason
            }.let {
                getById(it.value)!!
            }
        }

    override fun findNotEnd(id: String): Ban.PlayerBan? = transaction {
        Table.selectAll().where { (Table.ids like "%$${id}$%") and Table.endTime.greater(CurrentTimestamp) }
            .firstOrNull()?.toBan()
    }

    override fun delete(record: Int): Ban.PlayerBan? = transaction {
        getById(record)?.also {
            Table.deleteWhere { id eq record }
        }
    }
}

val rpcService = contextScript<coreLibrary.extApi.RpcService>()

onEnable {
    rpcService.register<Ban.PlayerBanStore> { ServiceImpl }
}