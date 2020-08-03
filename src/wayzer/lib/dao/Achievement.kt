package wayzer.lib.dao

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp

class Achievement : CacheEntity<Int>(T) {
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

    companion object : EntityClass<Int, Achievement>(::Achievement){
        @NeedTransaction
        fun newWithoutCache(init:Achievement.()->Unit) = Achievement().apply {
            init()
            save()
        }
    }
}