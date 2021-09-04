//avoid cycle depends
//@file:Depends("wayzer/user/level", "经验等级系统", soft = true)
//@file:Depends("wayzer/user/notification", "通知系统", soft = true)
//@file:Depends("wayzer/user/achievement", "成就系统", soft = true)

package wayzer.user

import cf.wayzer.placehold.PlaceHoldApi

fun getLevel(profile: PlayerProfile): Int {
    return PlaceHoldApi.GlobalContext.typeResolve(this, "level") as Int
}

fun updateExp(profile: PlayerProfile, exp: Int, desc: String = "") {
    val impl = depends("wayzer/user/level")?.import<(PlayerProfile, String, Int) -> Unit>("updateExp")
    if (impl == null) {
        logger.severe("updateExp(${profile.qq},$exp)")
        error("经验等级系统不可用,请联系管理员")
    }
    impl(profile, desc, exp)
}

/**
 * @param message 字符串模板
 * @param params 字符串变量,因为要存入数据库,仅支持字符串 例外自带变量player指向profile对应玩家
 * @param broadcast 在所有profile在线的服务器广播,否则只发给个人
 */
fun notify(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean = false) {
    val impl = depends("wayzer/user/notification")
        ?.import<(PlayerProfile, String, Map<String, String>, Boolean) -> Unit>("notify")
    if (impl == null) {
        logger.severe("finishAchievement(${profile.qq},$message,$params,$broadcast)")
        error("通知系统不可用,请联系管理员")
    }
    impl(profile, message, params, broadcast)
}

fun finishAchievement(profile: PlayerProfile, name: String, exp: Int, broadcast: Boolean = false) {
    val impl = depends("wayzer/user/achievement")
        ?.import<(PlayerProfile, String, Int, Boolean) -> Unit>("finishAchievement")
    if (impl == null) {
        logger.severe("finishAchievement(${profile.qq},$name,$exp,$broadcast)")
        error("成就系统不可用,请联系管理员")
    }
    impl(profile, name, exp, broadcast)
}
