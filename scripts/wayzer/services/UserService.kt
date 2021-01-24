package wayzer.services

import cf.wayzer.script_agent.define.annotations.ServiceDefine
import wayzer.lib.dao.PlayerProfile

@ServiceDefine
interface UserService {
    fun getLevel(profile: PlayerProfile): Int
    fun updateExp(profile: PlayerProfile, exp: Int, desc: String = "")

    /**
     * @param message 字符串模板
     * @param params 字符串变量,因为要存入数据库,仅支持字符串 例外自带变量player指向profile对应玩家
     * @param broadcast 在所有profile在线的服务器广播,否则只发给个人
     */
    fun notify(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean = false)
    fun finishAchievement(profile: PlayerProfile, name: String, exp: Int, broadcast: Boolean = false)
}