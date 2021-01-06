package coreMindustry.lib.compatibilities

import mindustry.entities.EntityGroup
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.net.NetConnection

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("Groups.player", "mindustry.gen.Groups"))
inline val playerGroup: EntityGroup<Player> get() = Groups.player

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层")
object Call {
    @Deprecated(
        "因为5.0已经弃用,逐渐摆脱兼容层",
        ReplaceWith("Call.sendMessage(con, msg, sender, senderPlayer)", "mindustry.gen.Call")
    )
    fun sendMessage(con: NetConnection?, msg: String, sender: String?, senderPlayer: Player?) =
        Call.sendMessage(con, msg, sender, senderPlayer)

    @Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("Call.infoMessage(con, message)", "mindustry.gen.Call"))
    fun onInfoMessage(con: NetConnection, message: String) = Call.infoMessage(con, message)

    @Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("Call.infoToast(con, message, duration)", "mindustry.gen.Call"))
    fun onInfoToast(con: NetConnection, message: String, duration: Float) = Call.infoToast(con, message, duration)
}