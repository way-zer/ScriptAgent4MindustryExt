package coreMindustry.lib.compatibilities

import mindustry.Vars
import mindustry.core.World
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.maps.Map

inline val Player.isAdmin get() = admin
inline val Player.isMobile get() = con?.mobile ?: false
inline val Player.uuid: String get() = uuid()
inline val Player.team: Team get() = team()

inline val World.map: Map get() = Vars.state.map