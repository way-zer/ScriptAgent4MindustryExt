package wayzer.lib.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp

class PlayerProfile(id:EntityID<Int>):IntEntity(id){
    var qq by T.qq
    var totalExp by T.totalExp
    var totalTime by T.totalTime //time in s
    var lastTime by T.lastTime
    object T:IntIdTable("PlayerProfile"){
        val qq = long("qq").index()
        val totalExp = integer("totalExp").default(0)
        val totalTime = integer("totalTime").default(0)
        val lastTime = timestamp("lastTime").defaultExpression(CurrentTimestamp())
    }
    companion object: IntEntityClass<PlayerProfile>(T)
}