@file:Import("@wayzer/services/UserService.kt", sourceFile = true)

package wayzer.user

import cf.wayzer.placehold.PlaceHoldApi
import wayzer.services.UserService

object Impl : UserService {
    lateinit var script: ISubScript
    override fun getLevel(profile: PlayerProfile): Int {
        return PlaceHoldApi.GlobalContext.typeResolve(this, "level") as Int
    }

    override fun updateExp(profile: PlayerProfile, exp: Int) {
        val impl = script.depends("wayzer/user/level")?.import<(PlayerProfile, Int) -> Unit>("updateExp")
        if (impl == null) {
            println("updateExp(${profile.qq},$exp)")
            error("经验等级系统不可用,请联系管理员")
        }
        impl(profile, exp)
    }

    override fun finishAchievement(profile: PlayerProfile, name: String, exp: Int, broadcast: Boolean) {
        val impl = script.depends("wayzer/user/achievement")
            ?.import<(PlayerProfile, String, Int, Boolean) -> Unit>("finishAchievement")
        if (impl == null) {
            println("finishAchievement(${profile.qq},$name,$exp,$broadcast)")
            error("成就系统不可用,请联系管理员")
        }
        impl(profile, name, exp, broadcast)
    }

    override fun notify(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean) {
        val impl = script.depends("wayzer/user/notification")
            ?.import<(PlayerProfile, String, Map<String, String>, Boolean) -> Unit>("notify")
        if (impl == null) {
            println("finishAchievement(${profile.qq},$message,$params,$broadcast)")
            error("通知系统不可用,请联系管理员")
        }
        impl(profile, message, params, broadcast)
    }
}
Impl.script = this
provide<UserService>(Impl)