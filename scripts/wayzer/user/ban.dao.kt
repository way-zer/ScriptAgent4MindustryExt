package wayzer.user

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import wayzer.lib.PlayerData
import java.time.Duration
import java.time.Instant

class PlayerBan(id: EntityID<Int>) : IntEntity(id) {
    var ids by T.ids
    var reason by T.reason
    var operator by T.operator
    val createTime by T.createTime
    var endTime by T.endTime

    object T : IntIdTable("PlayerBanV2") {
        val ids = text("ids", eagerLoading = true)
        val reason = text("reason", eagerLoading = true)
        val operator = text("operator").nullable()
        val createTime = timestamp("createTime").defaultExpression(CurrentTimestamp)
        val endTime = timestamp("endTime").defaultExpression(CurrentTimestamp)
    }

    companion object : IntEntityClass<PlayerBan>(T) {
        fun create(ids: PlayerData, time: Duration, reason: String, operator: String?): PlayerBan {
            return new {
                this.ids = ids.idsInDB
                endTime = Instant.now() + time
                this.operator = operator
                this.reason = reason
            }
        }

        fun allNotEnd() = find(T.endTime.greater(CurrentTimestamp))

        fun findNotEnd(id: String): PlayerBan? {
            return find { (T.ids like "%$${id}$%") and (T.endTime.greater(CurrentTimestamp)) }
                .firstOrNull()
        }
    }
}