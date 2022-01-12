package wayzer.lib.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.and
import wayzer.lib.dao.util.NeedTransaction
import java.time.Instant
import kotlin.math.max

class PlayerNotification(id: EntityID<Int>) : IntEntity(id) {
    var userId by T.user
    var message by T.message
    private var _params by T.params
    var params: Map<String, String>
        get() = _params.split("§").let {
            assert(it.size % 2 == 0)
            (0 until it.size / 2).associate { i -> it[i * 2] to it[i * 2 + 1] }
        }
        set(value) {
            _params = value.entries.asSequence().flatMap { listOf(it.key, it.value) }.joinToString("§")
        }
    var broadcast by T.broadcast
    var time by T.time

    object T : IntIdTable("PlayerNotification") {
        val user = reference("profile", PlayerProfile.T)
        val message = text("message", eagerLoading = true)
        val params = text("params", eagerLoading = true)//§分隔的k,v Map
        val broadcast = bool("broadcast")
        val time = timestamp("time").defaultExpression(CurrentTimestamp())
    }

    companion object : IntEntityClass<PlayerNotification>(T) {
        @NeedTransaction
        fun getNew(profile: PlayerProfile, lastTime: Instant) = find {
            (T.user eq profile.id) and (T.time greaterEq lastTime)
        }

        @NeedTransaction
        fun new(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean) = new {
            userId = profile.id
            this.message = message
            this.params = params
            this.broadcast = broadcast
        }
    }
}