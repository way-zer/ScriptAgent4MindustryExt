@file:Depends("private/towerDefend/TDDrop", "生成掉落物")
@file:Suppress("DuplicatedCode")

package mapScript

import mindustry.content.Items.*
import mindustry.content.UnitTypes.*
import mindustry.type.ItemStack.with

val dropMap = mapOf(
    // Spiders
    crawler to with(copper, 30, lead, 30, metaglass, 10),
    atrax to with(graphite, 15, silicon, 15, metaglass, 10),
    spiroct to with(
        silicon, 40, graphite, 40, thorium, 10,
        titanium, 10, surgeAlloy, 1, plastanium, 1, phaseFabric, 1,
    ),
    arkyid to with(plastanium, 30, phaseFabric, 30, surgeAlloy, 30),
    toxopid to with(
        copper, 400, lead, 400, silicon, 500, graphite, 500, thorium, 500, titanium, 500,
        plastanium, 50, phaseFabric, 50, surgeAlloy, 50,
    ),
    // Shooters
    dagger to with(copper, 30, lead, 30, metaglass, 10),
    mace to with(graphite, 15, silicon, 15),
    fortress to with(
        silicon, 40, graphite, 40, thorium, 10, titanium, 10,
        surgeAlloy, 1, plastanium, 1, phaseFabric, 1,
    ),
    scepter to with(plastanium, 30, phaseFabric, 30, surgeAlloy, 30),
    reign to with(
        copper, 400, lead, 400, silicon, 500, graphite, 500, thorium, 500, titanium, 500,
        plastanium, 50, phaseFabric, 50, surgeAlloy, 50,
    ),
    // Laze ers
    nova to with(copper, 20, lead, 10, metaglass, 3),
    pulsar to with(copper, 30, lead, 40, metaglass, 10),
    quasar to with(silicon, 40, graphite, 40, thorium, 10, titanium, 10, surgeAlloy, 1, plastanium, 1, phaseFabric, 1),
    vela to with(plastanium, 20, phaseFabric, 15, surgeAlloy, 10),
    corvus to with(
        copper, 400, lead, 400, silicon, 100, metaglass, 120,
        graphite, 100, titanium, 120, thorium, 60, phaseFabric, 10, surgeAlloy, 10,
    ),
    // Flyers
    flare to with(coal, 5),
    horizon to with(pyratite, 10, coal, 5),
    zenith to with(pyratite, 15, coal, 20, blastCompound, 10),
    antumbra to with(pyratite, 30, coal, 50, blastCompound, 25),
    eclipse to with(pyratite, 50, coal, 100, blastCompound, 50, surgeAlloy, 50),
    // Support
    mono to with(copper, 20, lead, 10, silicon, 3),
    poly to with(copper, 30, lead, 40, silicon, 10, titanium, 5),
    mega to with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
    quad to with(copper, 300, silicon, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
    oct to with(
        copper, 400, lead, 400, silicon, 120, graphite, 120,
        thorium, 40, plastanium, 40, phaseFabric, 5, surgeAlloy, 15
    ),
    // Ships
    risso to with(copper, 20, lead, 10, metaglass, 3),
    minke to with(copper, 30, lead, 40, metaglass, 10),
    bryde to with(lead, 100, metaglass, 40, silicon, 40, titanium, 80, thorium, 10),
    sei to with(copper, 300, metaglass, 80, graphite, 80, titanium, 60, plastanium, 20, surgeAlloy, 5),
    omura to with(
        copper, 400, lead, 400, silicon, 100, metaglass, 120,
        graphite, 100, titanium, 120, thorium, 60, phaseFabric, 10, surgeAlloy, 10
    ),
    // Basic
    alpha to with(copper, 5, lead, 10, silicon, 2, graphite, 3, metaglass, 3),
    beta to with(copper, 8, silicon, 5),
    gamma to with(lead, 10, metaglass, 8, graphite, 8),
)

val tdDrop = contextScript<private.towerDefend.TDDrop>()

listen<EventType.UnitDestroyEvent> { e ->
    val unit = e.unit
    //注释掉if可对所有队伍单位开启
    if (unit.team == state.rules.waveTeam)
        tdDrop.handleDrop(unit, dropMap[unit.type])
}