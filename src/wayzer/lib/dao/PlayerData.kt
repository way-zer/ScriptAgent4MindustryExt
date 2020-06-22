package wayzer.lib.dao

import mindustry.entities.type.Player
import mindustry.net.Administration
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp

class PlayerData(id: EntityID<String>) : Entity<String>(id) {
    var lastName by T.lastName
    var firstIP by T.firstIp
    var lastIp by T.lastIp
    var firstTime by T.firstTime
    var lastTime by T.lastTime
    var profile by PlayerProfile optionalReferencedOn T.profile

    object T : IdTable<String>() {
        override val id: Column<EntityID<String>> = varchar("uid", 24).entityId()
        override val primaryKey: PrimaryKey = PrimaryKey(id)
        val lastName = varchar("lastName", 48)
        val firstIp = varchar("firstIp", 15)
        val lastIp = varchar("lastIp", 15)
        val firstTime = timestamp("firstTime").defaultExpression(CurrentTimestamp())
        val lastTime = timestamp("lastTime").defaultExpression(CurrentTimestamp())
        val profile = optReference("profile", PlayerProfile.T)
    }

    companion object : EntityClass<String, PlayerData>(T) {
        operator fun get(p: Player) = findById(p.uuid) ?: new(p.uuid) {
            lastName = p.name
            firstIP = p.con.address
            lastIp = p.con.address
        }
        operator fun get(p: Administration.PlayerInfo) = findById(p.id)?: new(p.id) {
            lastName = p.lastName
            firstIP = p.lastIP
            lastIp = p.lastIP
        }
    }
}