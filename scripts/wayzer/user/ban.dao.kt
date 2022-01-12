package wayzer.user

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.PlayerProfile
import wayzer.lib.dao.util.NeedTransaction
import java.time.Duration
import java.time.Instant

class PlayerBan(id: EntityID<Int>) : IntEntity(id) {
    var profileId by T.profile
    var reason by T.reason
    var operator by T.operator
    val createTime by T.createTime
    var endTime by T.endTime
//    var undo by T.undo

    object T : IntIdTable("PlayerBan") {
        val profile = reference("profile", PlayerProfile.T)
        val reason = text("reason", eagerLoading = true)
        val operator = reference("operator", PlayerProfile.T).nullable()
        val createTime = timestamp("createTime").defaultExpression(CurrentTimestamp())
        val endTime = timestamp("endTime").defaultExpression(CurrentTimestamp())
//        val undo = bool("undo").default(false)
    }

    companion object : IntEntityClass<PlayerBan>(T) {
        fun <T> transactionOrExisted(body: Transaction.() -> T): T {
            val now = TransactionManager.currentOrNull()
            return if (now != null) body(now)
            else transaction { body() }
        }

        fun create(profile: PlayerProfile, time: Duration, reason: String, operator: PlayerProfile?): PlayerBan {
            return transactionOrExisted {
                findNotEnd(profile.id)?.let { return@transactionOrExisted it }
                new {
                    profileId = profile.id
                    endTime = Instant.now() + time
                    this.operator = operator?.id
                    this.reason = reason
                }
            }
        }

        @NeedTransaction
        fun allNotEnd() = find(T.endTime.greater(CurrentTimestamp()))

        fun findNotEnd(profileId: EntityID<Int>): PlayerBan? {
            return transactionOrExisted {
                find { (T.profile eq profileId) and (T.endTime.greater(CurrentTimestamp())) }
                    .firstOrNull()
            }
        }
    }
}