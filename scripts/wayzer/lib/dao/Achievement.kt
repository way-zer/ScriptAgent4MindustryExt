package wayzer.lib.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import wayzer.lib.dao.util.NeedTransaction

class Achievement(id: EntityID<Int>) : IntEntity(id) {
    var userId by T.profile
    var name by T.name
    var exp by T.exp
    var time by T.time

    object T : IntIdTable("Achievement") {
        val profile = reference("profile", PlayerProfile.T)
        val name = varchar("name", 64)
        val exp = integer("exp")
        val time = timestamp("time").defaultExpression(CurrentTimestamp())
    }

    companion object : IntEntityClass<Achievement>(T) {
        @NeedTransaction
        fun newWithCheck(profile: EntityID<Int>, name: String, exp: Int): Boolean {
            return if (T.select { (T.profile eq profile) and (T.name eq name) }.empty()) {
                new {
                    userId = profile
                    this.name = name
                    this.exp = exp
                }
                true
            } else false
        }
    }
}