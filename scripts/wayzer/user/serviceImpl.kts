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
        val impl = script.depends("wayzer/user/expReward")?.import<(PlayerProfile, Int) -> Unit>("updateExp")
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

    override fun notice(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean) {
        TODO("通知系统")
    }
}
Impl.script = this
provide<UserService>(Impl)