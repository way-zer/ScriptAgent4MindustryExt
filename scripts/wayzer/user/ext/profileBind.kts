@file:Import("@wayzer/services/UserService.kt", sourceFile = true)

package wayzer.user.ext

import cf.wayzer.placehold.PlaceHoldApi.with
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.services.UserService
import java.time.Duration
import java.util.*
import kotlin.random.Random

export(::generate)// 生成绑定码
export(::check)// 检测绑定码

class ExpireMutableMap<K, V> {
    class ExpireItem<V>(val time: Long, val v: V) : Comparable<ExpireItem<V>> {
        override fun compareTo(other: ExpireItem<V>): Int {
            return compareValuesBy(this, other) { it.time }
        }
    }

    private val map = mutableMapOf<K, V>()
    private val expireQueue = PriorityQueue<ExpireItem<K>>()
    fun add(expireTime: Long, key: K, value: V): Boolean {
        if (key in this) return false
        map[key] = value
        return expireQueue.add(ExpireItem(System.currentTimeMillis() + expireTime, key))
    }

    fun checkOut() {
        while (true) {
            val item = expireQueue.peek() ?: break
            if (item.time >= System.currentTimeMillis()) break
            expireQueue.poll()
            map.remove(item.v)
        }
    }

    operator fun get(key: K): V? {
        checkOut()
        return map[key]
    }

    fun removeValue(v: V) {
        map.entries.removeIf { it.value == v }
    }

    private operator fun contains(key: K): Boolean = get(key) != null
}

val expireTime: Duration by config.key(Duration.ofMinutes(10), "随机绑定码到期时间")
val map = ExpireMutableMap<Int, Long>()
val userService by ServiceRegistry<UserService>()

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

onEnable {
    launch {
        while (true) {
            delay(60_000)
            map.checkOut()
        }
    }
}

command("genCode", "管理指令: 为用户生成随机绑定码") {
    usage = "<qq>"
    permission = "wayzer.user.genCode"
    body {
        val qq = arg.firstOrNull()?.toLongOrNull() ?: returnReply("[red]请输入正确的qq号".with())
        reply("[green]绑定码{code},有效期:{expireTime}".with("code" to generate(qq), "expireTime" to expireTime))
    }
}

command("bind", "绑定用户") {
    usage = "<六位code>";this.type = CommandType.Client
    body {
        val qq = arg.firstOrNull()?.toIntOrNull()?.let(::check)
            ?: returnReply("[red]请输入正确的6位绑定码,如没有，可找群内机器人获取".with())
        PlayerData[player!!.uuid()].apply {
            if (profile != null && (profile!!.qq != qq || secure(player!!)))
                returnReply("[red]你已经绑定用户，如需解绑，请联系管理员".with())
            transaction {
                bind(player!!, PlayerProfile.findOrCreate(qq).apply {
                    this.onJoin(player!!)
                })
            }
            userService.finishAchievement(profile!!, "绑定账号", 100, false)
            userService.updateExp(profile!!, 0)
        }
        reply("[green]绑定账号[yellow]$qq[green]成功.".with())
    }
}
