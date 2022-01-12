package wayzer.lib.dao

import arc.util.Strings
import com.google.common.cache.CacheBuilder
import coreLibrary.lib.with
import coreMindustry.lib.sendMessage
import mindustry.gen.Player
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
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
    private var profileId by T.profile
    val profile: PlayerProfile? get() = profileId?.let { PlayerProfile[it] }

    @NeedTransaction
    fun onJoin(player: Player) {
        if (secure(player)) {
            lastName = Strings.stripColors(player.name)
            lastTime = Instant.now()
            lastIp = player.con.address
            profile?.onJoin(player)
        } else player.sendMessage("[red]检测到账号不安全,请重新绑定进行验证".with())
    }

    @NeedTransaction
    fun onQuit(player: Player) {
        if (secure(player)) {
            lastTime = Instant.now()
            lastIp = player.con.address
        }
        secureProfile(player)?.onQuit(player)
    }

    @NeedTransaction
    fun bind(player: Player, profile: PlayerProfile) {
        this.profileId = profile.id
        Usid.put(this, player.usid())
    }

    fun secure(player: Player): Boolean {
        if (!Setting.checkUsid || profileId == null) return true
        return Usid.get(this, player) == player.usid()
    }

    fun secureProfile(player: Player) = if (secure(player)) profile else null

    object T : IdTable<String>("PlayerData") {
        override val id: Column<EntityID<String>> = varchar("UID", 24).entityId()
        override val primaryKey: PrimaryKey = PrimaryKey(id)
        val lastName = varchar("lastName", 48)
        val lastIp = varchar("lastIp", 15)
        val firstIp = varchar("firstIp", 15)
        val lastTime = timestamp("lastTime").defaultExpression(CurrentTimestamp())
        val firstTime = timestamp("firstTime").defaultExpression(CurrentTimestamp())
        val profile = optReference("profile", PlayerProfile.T)
    }

    @Suppress("RemoveRedundantQualifierName", "MemberVisibilityCanBePrivate")
    object Usid : IntIdTable("PlayerUsid") {
        val user = reference("user", T)
        val server = varchar("server", 16)
        val usid = varchar("sid", 12)

        private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build<String, String>()//uuid -> usid

        @NeedTransaction
        fun put(user: PlayerData, usid: String) {
            if (!Setting.tempServer) {
                val found = Usid.update({ (Usid.user eq user.id) and (Usid.server eq Setting.serverId) }) {
                    it[Usid.usid] = usid
                }
                if (found == 0) Usid.insert {
                    it[Usid.user] = user.id
                    it[Usid.server] = Setting.serverId
                    it[Usid.usid] = usid
                }
            }
            cache.put(user.id.value, usid)
        }

        fun get(user: PlayerData, player: Player): String? {
            return cache.getIfPresent(user.id.value) ?: when {
                Setting.quickLogin && player.con.address == user.lastIp -> transaction {
                    put(user, player.usid())
                    player.usid()
                }
                Setting.tempServer -> null
                else -> transaction {
                    Usid.select { (Usid.user eq user.id) and (Usid.server eq Setting.serverId) }.singleOrNull()
                        ?.get(Usid.usid)
                        ?.also { cache.put(user.id.value, it) }
                }
            }
        }
    }

    companion object : EntityClass<String, PlayerData>(T) {
        private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build<String, PlayerData>()

        fun findOrCreate(p: Player) = findById(p.uuid()) ?: transaction {
            new(p.uuid()) {
                firstIP = p.con.address
                lastIp = p.con.address
                lastName = Strings.stripColors(p.name)
            }.also { it.flush() }
        }.also { cache.put(p.uuid(), it) }

        override fun findById(id: EntityID<String>): PlayerData? = cache.getIfPresent(id.value) ?: transaction {
            super.findById(id)
        }?.also { cache.put(id.value, it) }
    }
}