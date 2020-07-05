package wayzer.user

import cf.wayzer.placehold.PlaceHoldApi.with
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

export(::generate)// 生成绑定码
export(::check)// 检测绑定码

class ExpireMutableMap<K, V>() {
    private val map = mutableMapOf<K, V>()
    private val expireQueue = PriorityQueue<Pair<Long, K>>()
    fun add(expireTime: Long, key: K, value: V): Boolean {
        if (key in this) return false
        map[key] = value
        return expireQueue.add(System.currentTimeMillis() + expireTime to key)
    }

    operator fun get(key: K): V? {
        expireQueue.peek().takeIf { it.first > System.currentTimeMillis() }?.let {
            expireQueue.poll()
            map.remove(key)
        }
        return map[key]
    }
    operator fun contains(key:K):Boolean = get(key) != null
}

val expireTime: Duration by config.key(Duration.ofMinutes(10), "随机绑定码到期时间")
val map = ExpireMutableMap<Int, Long>()

fun generate(qq: Long): Int {
    var code: Int
    do {
        code = Random.nextInt(1000000)
    } while (map.add(expireTime.toMillis(), code, qq))
    return code
}

fun check(code: Int): Long? {
    return map[code]
}

command("genCode", "管理指令: 为用户生成随机绑定码", "<qq>", CommandType.Server) { arg, p ->
    val qq = arg.firstOrNull()?.toLongOrNull() ?: return@command p.sendMessage("[red]请输入正确的qq号")
    p.sendMessage("[green]绑定码{code},有效期:{expireTime}".with("code" to generate(qq), "expireTime" to expireTime))
}

command("bind", "绑定用户", "<六位code>", CommandType.Client) { arg, p ->
    val qq = arg.firstOrNull()?.toIntOrNull()?.let(::check)
            ?: return@command p!!.sendMessage("[red]请输入正确的6位绑定码,如没有，可找群内机器人获取")
    transaction {
        PlayerData[p!!].apply {
            if(profile != null)
                return@transaction p.sendMessage("[red]你已经绑定用户，如需解绑，请联系管理员")
            profile = PlayerProfile.getOrCreate(qq).apply {
                lastTime = Instant.now()
            }
        }
    }
    p.sendMessage("[green]绑定账号[yellow]$qq[green]成功.")
}