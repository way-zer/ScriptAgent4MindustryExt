package wayzer.lib.dao

import mindustry.gen.Player
import mindustry.net.Administration
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

class PlayerData : CacheEntity<String>(T) {
    var lastName by T.lastName
    var firstIP by T.firstIp
    var lastIp by T.lastIp
    var firstTime by T.firstTime
    var lastTime by T.lastTime
    private var profileId by T.profile
    var profile: PlayerProfile? = null

    @NeedTransaction
    override fun load(resultRow: ResultRow): Boolean {
        if (!super.load(resultRow)) return false
        if (profileId == null) return true
        profile = PlayerProfile.getOrFind(profileId!!.value,cache = true)
        return true
    }

    @NeedTransaction
    override fun save(id: String?) {
        profile?.save()
        profileId = profile?.id
        super.save(id)
    }

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

    /**
     *
     */
    companion object : EntityClass<String, PlayerData>(::PlayerData) {
        //给在线玩家使用，假设数据已经在缓存中
        operator fun get(uuid: String) = getOrNull(uuid) ?: error("initFirst")
        override fun removeCache(id: String): PlayerData? {
            return super.removeCache(id)?.apply {
                if (profile != null && allCached.none { it.profile == profile }) {
                    PlayerProfile.removeCache(profile!!.id.value)
                }
            }
        }

        @NeedTransaction
        //给新进入玩家使用，自动添加进入缓存
        fun findOrCreate(p: Player) = findOrCreate(p.uuid(), cache = true) {
            firstIP = p.con.address
            lastIp = p.con.address
            lastName = p.name
            save(p.uuid())
        }.apply {
            lastIp = p.con.address
            lastName = p.name
            lastTime = Instant.now()
            profile?.lastTime = Instant.now()
        }

        @NeedTransaction
        //供一次性查询使用，不缓存
        fun find(p: Administration.PlayerInfo) = getOrFind(p.id,cache = false)
    }
}