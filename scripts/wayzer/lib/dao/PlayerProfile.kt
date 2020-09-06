package wayzer.lib.dao

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.CurrentTimestamp
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select

class PlayerProfile:CacheEntity<Int>(T){
    var qq by T.qq
    var totalExp by T.totalExp
    var totalTime by T.totalTime //time in s
    var registerTime by T.registerTime
    var lastTime by T.lastTime
    object T:IntIdTable("PlayerProfile"){
        val qq = long("qq").uniqueIndex()
        val totalExp = integer("totalExp").default(0)
        val totalTime = integer("totalTime").default(0)
        val registerTime = timestamp("registerTime").defaultExpression(CurrentTimestamp())
        val lastTime = timestamp("lastTime").defaultExpression(CurrentTimestamp())
    }
    companion object:EntityClass<Int,PlayerProfile>(::PlayerProfile){
        @NeedTransaction
        //绑定账号或后台查询使用，可选是否缓存
        fun getOrCreate(qq:Long,cache:Boolean) = allCached.find { it.qq == qq }?:let {
            val result = T.select{T.qq eq qq}.firstOrNull()
            val o = PlayerProfile()
            if(result!=null)o.load(result)
            else {
                o.qq = qq
                o.save()
            }
            if(cache) addCache(o)
            o
        }
    }
}