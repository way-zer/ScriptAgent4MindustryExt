package wayzer.user

import cf.wayzer.placehold.PlaceHoldApi.with
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

export(::generate)// 生成绑定码
export(::check)// 检测绑定码

class ExpireMutableMap<K, V> {
    class ExpireItem<V>(val time:Long,val v:V):Comparable<ExpireItem<V>>{
        override fun compareTo(other: ExpireItem<V>): Int {
            return compareValuesBy(this,other){it.time}
        }
    }
    private val map = mutableMapOf<K, V>()
    private val expireQueue = PriorityQueue<ExpireItem<K>>()
    fun add(expireTime: Long, key: K, value: V): Boolean {
        if (key in this) return false
        map[key] = value
        return expireQueue.add(ExpireItem(System.currentTimeMillis() + expireTime,key))
    }

    operator fun get(key: K): V? {
        expireQueue.peek()?.takeIf { it.time < System.currentTimeMillis() }?.let {
            expireQueue.poll()
            map.remove(it.v)
        }
        return map[key]
    }
    fun removeValue(v:V){
        map.entries.removeIf{it.value==v}
    }
    private operator fun contains(key:K):Boolean = get(key) != null
}

val expireTime: Duration by config.key(Duration.ofMinutes(10), "随机绑定码到期时间")
val map = ExpireMutableMap<Int, Long>()

fun generate(qq: Long): Int {
    map.removeValue(qq)
    var code: Int
    do {
        code = Random.nextInt(1000000)
    } while (!map.add(expireTime.toMillis(), code, qq))
    return code
}

fun check(code: Int): Long? {
    return map[code]
}

command("genCode", "管理指令: 为用户生成随机绑定码", {
    this.usage = "<qq>"
    permission = "wayzer.user.genCode"
}) {
    val qq = arg.firstOrNull()?.toLongOrNull() ?: return@command reply("[red]请输入正确的qq号".with())
    reply("[green]绑定码{code},有效期:{expireTime}".with("code" to generate(qq), "expireTime" to expireTime))
}

command("bind", "绑定用户", {
    this.usage = "<六位code>";this.type = CommandType.Client
}) {
    val qq = arg.firstOrNull()?.toIntOrNull()?.let(::check)
            ?: return@command reply("[red]请输入正确的6位绑定码,如没有，可找群内机器人获取".with())
    PlayerData[player!!.uuid].apply {
        if (profile != null)
            return@command reply("[red]你已经绑定用户，如需解绑，请联系管理员".with())
        @Suppress("EXPERIMENTAL_API_USAGE")
        transaction {
            profile = PlayerProfile.getOrCreate(qq, true).apply {
                lastTime = Instant.now()
            }
            save()
        }
        val finishAchievement = depends("wayzer/user/achievement")?.import<(PlayerProfile, String, Int, Boolean) -> Unit>("finishAchievement")
        finishAchievement?.invoke(profile!!, "绑定账号", 100, false)
    }
    reply("[green]绑定账号[yellow]$qq[green]成功.".with())
}
