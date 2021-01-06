package coreMindustry.lib.compatibilities

import mindustry.Vars
import mindustry.core.World
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.maps.Map

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("admin"))
inline val Player.isAdmin
    get() = admin

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("con?.mobile==true"))
inline val Player.isMobile
    get() = con?.mobile == true

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("uuid()"))
inline val Player.uuid: String
    get() = uuid()

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("team()"))
inline val Player.team: Team
    get() = team()

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("Vars.state.map", "mindustry.Vars"))
inline val World.map: Map
    get() = Vars.state.map