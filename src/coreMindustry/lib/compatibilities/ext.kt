package coreMindustry.lib.compatibilities

import mindustry.Vars
import mindustry.core.World
import mindustry.gen.Player
import mindustry.maps.Map

inline val Player.isAdmin get() = admin
inline val Player.isMobile get() = con?.mobile ?: false

inline val World.map: Map get() = Vars.state.map