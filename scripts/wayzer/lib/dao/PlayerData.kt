package wayzer.lib.dao

import com.google.common.cache.CacheBuilder
import coreLibrary.lib.with
import coreMindustry.lib.sendMessage
import mindustry.gen.Player
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.util.NeedTransaction
import java.time.Duration
import java.time.Instant

class PlayerData(id: EntityID<String>) : Entity<String>(id) {
    var lastName by T.lastName
    var firstIP by T.firstIp
    var lastIp by T.lastIp
    var firstTime by T.firstTime
    var lastTime by T.lastTime
    var profileId by T.profile
    val profile: PlayerProfile? get() = profileId?.let { PlayerProfile[it] }
    var sid by T.sid

    @NeedTransaction
    fun onJoin(player: Player) {
        lastName = player.name
        lastIp = player.con.address
        lastTime = Instant.now()
        if (!secure(player)) player.sendMessage("[red]检测到账号不安全,请重新绑定进行验证. 否则无法使用该账号的权限".with())
        profile?.onJoin(player)
    }

    @NeedTransaction
    fun onQuit(player: Player) {
        profile?.onQuit(player)
    }

    @NeedTransaction
    fun bind(player: Player, profile: PlayerProfile) {
        this.profileId = profile.id
        sid = player.usid()
    }

    fun secure(player: Player) = player.usid() == sid

    object T : IdTable<String>("PlayerData") {
        override val id: Column<EntityID<String>> = varchar("UID", 24).entityId()
        override val primaryKey: PrimaryKey = PrimaryKey(id)
        val lastName = varchar("lastName", 48)
        val lastIp = varchar("lastIp", 15)
        val firstIp = varchar("firstIp", 15)
        val lastTime = timestamp("lastTime").defaultExpression(CurrentTimestamp())
        val firstTime = timestamp("firstTime").defaultExpression(CurrentTimestamp())
        val profile = optReference("profile", PlayerProfile.T)
        val sid = varchar("sid", 12)
    }

    companion object : EntityClass<String, PlayerData>(T) {
        private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build<String, PlayerData>()

        fun findOrCreate(p: Player) = findById(p.uuid()) ?: transaction {
            new {
                firstIP = p.con.address
                lastIp = p.con.address
                lastName = p.name
                sid = p.usid()
            }
        }.also { cache.put(p.uuid(), it) }

        override fun findById(id: EntityID<String>): PlayerData? = cache.get(id.value) {
            transaction {
                super.findById(id)
            }
        }
    }
}