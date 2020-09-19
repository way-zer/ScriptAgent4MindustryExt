package coreMindustry.lib.compatibilities

import mindustry.entities.EntityGroup
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.net.NetConnection

inline val playerGroup: EntityGroup<Player> get() = Groups.player

object Call {
    fun sendMessage(con: NetConnection?, msg: String, sender: String?, senderPlayer: Player?) = Call.sendMessage(con, msg, sender, senderPlayer)
    fun onInfoMessage(con: NetConnection, message: String) = Call.infoMessage(con, message)
    fun onInfoToast(con: NetConnection, message: String, duration: Float) = Call.infoToast(con, message, duration)
}