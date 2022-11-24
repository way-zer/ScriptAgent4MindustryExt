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
import wayzer.lib.dao.util.TransactionHelper
import wayzer.lib.dao.util.WithTransactionHelper
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

    @Transient
    var player: Player? = null
    var realName = "NotInit"

    @WithTransactionHelper
    fun onJoin(player: Player) {
        if (this.player == player) return
        if (this.player != null)
            return player.kick("[red]你已经在服务器中了")
        this.player = player
        if (secure(player)) {
            TransactionHelper.lateUpdate {
                lastName = Strings.stripColors(realName)
                lastTime = Instant.now()
                lastIp = player.con.address
            }
            profile?.onJoin(player)
        } else player.sendMessage("[red]检测到账号不安全,请重新绑定进行验证".with())
    }

    @WithTransactionHelper
    fun onQuit(player: Player) {
        this.player = null
        if (secure(player)) TransactionHelper.lateUpdate {
            lastTime = Instant.now()
            lastIp = player.con.address
        }
        secureProfile(player)?.onQuit(player)
    }

    @NeedTransaction
    fun bind(player: Player?, profile: PlayerProfile) {
        this.profileId = profile.id
        if (player != null)
            Usid.put(this, player.usid())
    }

    fun secure(player: Player): Boolean {
        if (!Setting.checkUsid || profileId == null) return true
        val secure = Usid.get(this, player)
        if (secure == null) {
            transaction {
                Usid.put(this@PlayerData, player.usid())
            }
            return true
        }
        return secure == player.usid()
    }

    fun secureProfile(player: Player) = if (secure(player)) profile else null

    object T : IdTable<String>("PlayerData") {
        override val id: Column<EntityID<String>> = varchar("UID", 24).entityId()
        override val primaryKey: PrimaryKey = PrimaryKey(id)
        val lastName = varchar("lastName", 32)
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
            .expireAfterAccess(Duration.ofMinutes(60))
            .build<String, String>()//uuid -> usid

        @NeedTransaction
        fun put(user: PlayerData, usid: String) {
            if (!Setting.checkUsid) return
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
        private val realCache = mutableMapOf<String, PlayerData>()
        private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .removalListener<String, PlayerData> {
                if (it.value!!.player == null)
                    realCache.remove(it.key)
            }
            .build<String, PlayerData>()

        private fun putCache(id: String, v: PlayerData) {
            realCache[id] = v
            cache.put(id, v)
        }

        @NeedTransaction
        fun findOrCreate(uuid: String, address: String, name: String) =
            findByIdWithTransaction(uuid) ?: transaction {
                new(uuid) {
                    firstIP = address
                    lastIp = address
                    lastName = Strings.stripColors(name)
                }.also { it.flush() }
            }.also { putCache(uuid, it) }

        /**Must call after findOrCreate or null*/
        override fun findById(id: EntityID<String>): PlayerData? = cache.getIfPresent(id.value)
            ?: realCache[id.value]?.also { cache.put(id.value, it) }

        @NeedTransaction
        fun findByIdWithTransaction(id: String) = findById(id) ?: transaction {
            super.findById(EntityID(id, T))
        }?.also { putCache(id, it) }
    }
}