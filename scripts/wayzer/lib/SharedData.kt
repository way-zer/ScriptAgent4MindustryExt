package wayzer.lib

import arc.files.Fi
import mindustry.entities.type.Player
import mindustry.game.Gamemode
import mindustry.maps.Map

object SharedData {
    interface IAdmin{
        fun isAdmin(player:Player):Boolean
        fun ban(player: Player, uuid: String)
        fun secureLog(tag: String, text: String)
    }
    lateinit var admin: IAdmin
}