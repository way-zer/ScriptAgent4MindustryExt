package wayzer.lib.dao

import cf.wayzer.script_agent.getContextModule
import coreLibrary.lib.config
import kotlin.random.Random

object Setting {
    private val moduleConfig = Setting::class.java.getContextModule()!!.config
    val checkUsid by moduleConfig.key(true, "是否检查玩家usid", "检查usid在某些情况,玩家可能需要频繁登录")
    val quickLogin by moduleConfig.key(false, "基于ip比对的快速登录", "可用于群组跨服验证", "需要服务器能够获取玩家真实ip")
    val limitOne by moduleConfig.key(false, "仅允许玩家同时在一个服务器登录", "可以避免一些潜在bug")

    private val randomHash = Random.nextBytes(3).joinToString("") { it.toString(16) }//6位hash
    val tempServer by moduleConfig.key(
        false,
        "是否是动态扩容服务器",
        "开启后,serverId将在Name后添加随机hash,不再往数据库记录usid",
        "通常应该开启quickLogin，提高用户体验"
    )
    private val serverName by moduleConfig.key("default", "服务器标识,用于多服务器群组情况")
    val serverId: String
        get() = if (tempServer) serverName + randomHash
        else serverName
}