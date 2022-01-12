package wayzer.lib.dao

import arc.util.Strings
import com.google.common.cache.CacheBuilder
import mindustry.gen.Player
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.util.NeedTransaction
import java.time.Duration
import java.time.Instant

class PlayerProfile(id: EntityID<Int>) : IntEntity(id) {
    var qq by T.qq
    var name by T.lastName
    var totalExp by T.totalExp
    var totalTime by T.totalTime //time in s
    var registerTime by T.registerTime
    var lastTime by T.lastTime
    var lang by T.lang
    var online by T.online

    val controlling get() = online == Setting.serverId

    @NeedTransaction
    fun onJoin(player: Player) {
        if (online == null) online = Setting.serverId
        if (controlling) {
            name = Strings.stripColors(player.name)
        }
        if (!controlling) {
            if (Setting.limitOne) player.kick("[red]你已经在其他服务器登录,禁止再在该服登录")
            else player.sendMessage("[yellow]你已经在其他服务器登录，不重复累计在线时长")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @NeedTransaction
    fun onQuit(player: Player) {
        if (controlling) {
            lastTime = Instant.now()
            online = null
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other as? PlayerProfile)?.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    object T : IntIdTable("PlayerProfile") {
        val qq = long("qq").uniqueIndex()
        val lastName = varchar("lastName", length = 32).nullable()
        val totalExp = integer("totalExp").default(0)
        val totalTime = integer("totalTime").default(0)
        val lang = varchar("lang", length = 16).nullable()
        val online = varchar("server", length = 16).nullable()
        val registerTime = timestamp("registerTime").defaultExpression(CurrentTimestamp())
        val lastTime = timestamp("lastTime").defaultExpression(CurrentTimestamp())
    }

    companion object : IntEntityClass<PlayerProfile>(T) {
        private val cache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(10))
            .build<Int, PlayerProfile>()
        private val idCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build<Long, Int>()//qq to id

        override fun findById(id: EntityID<Int>): PlayerProfile? {
            return cache.getIfPresent(id.value) ?: transaction {
                super.findById(id)
            }?.also { cache.put(id.value, it) }
        }

        fun findByQQ(qq: Long): PlayerProfile? {
            val id = idCache.getIfPresent(qq)
            return if (id != null) findById(id)
            else transaction {
                find { T.qq eq qq }.singleOrNull()?.also {
                    cache.put(it.id.value, it)
                    idCache.put(it.qq, it.id.value)
                }
            }
        }

        fun findOrCreate(qq: Long) = findByQQ(qq) ?: transaction {
            new {
                this.qq = qq
            }.also { it.flush() }
        }.also {
            cache.put(it.id.value, it)
            idCache.put(it.qq, it.id.value)
        }
    }
}