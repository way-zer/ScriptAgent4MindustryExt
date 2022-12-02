package private.towerDefend

import coreMindustry.lib.listen
import mindustry.Vars.state
import mindustry.content.Items.*
import mindustry.content.UnitTypes.*
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Unit
import mindustry.type.ItemStack
import mindustry.type.ItemStack.with
import kotlin.random.Random

val dropMap = mapOf(
    // Ground Attack
    dagger to with(copper, 20, lead, 10, silicon, 3),
    mace to with(copper, 30, lead, 40, silicon, 10, titanium, 5),
    fortress to with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
    scepter to with(copper, 300, silicon, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
    reign to with(
        copper, 400, lead, 400, silicon, 120, graphite, 120, thorium, 40,
        plastanium, 40, phaseFabric, 5, surgeAlloy, 15
    ),
    // Spiders
    crawler to with(copper, 20, lead, 10, graphite, 3),
    atrax to with(copper, 30, lead, 40, graphite, 10, titanium, 5),
    spiroct to with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
    arkyid to with(copper, 300, graphite, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
    toxopid to with(
        copper, 400, lead, 400, silicon, 120, graphite, 120, thorium, 40,
        plastanium, 40, phaseFabric, 5, surgeAlloy, 15
    ),
    // Ground Support
    nova to with(copper, 20, lead, 10, titanium, 3),
    pulsar to with(copper, 30, lead, 40, titanium, 10),
    quasar to with(lead, 100, metaglass, 40, silicon, 40, titanium, 80, thorium, 10),
    vela to with(copper, 300, metaglass, 80, graphite, 80, titanium, 60, plastanium, 20, surgeAlloy, 5),
    corvus to with(
        copper, 400, lead, 400, silicon, 100, metaglass, 120,
        graphite, 100, titanium, 120, thorium, 60, phaseFabric, 10, surgeAlloy, 10
    ),
    // Air Attack
    flare to with(copper, 20, lead, 10, graphite, 3),
    horizon to with(copper, 30, lead, 40, graphite, 10),
    zenith to with(lead, 100, silicon, 40, graphite, 40, titanium, 30, plastanium, 10),
    antumbra to with(copper, 300, graphite, 80, metaglass, 80, titanium, 60, surgeAlloy, 15),
    eclipse to with(
        copper, 400, lead, 400, silicon, 120, graphite, 120,
        titanium, 120, thorium, 40, plastanium, 40, phaseFabric, 10, surgeAlloy, 5
    ),
    // Air Support
    mono to with(copper, 20, lead, 10, silicon, 3),
    poly to with(copper, 30, lead, 40, silicon, 10, titanium, 5),
    mega to with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
    quad to with(copper, 300, silicon, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
    oct to with(
        copper, 400, lead, 400, silicon, 120, graphite, 120,
        thorium, 40, plastanium, 40, phaseFabric, 5, surgeAlloy, 15
    ),
    // Ship Attack
    risso to with(copper, 20, lead, 10, metaglass, 3),
    minke to with(copper, 30, lead, 40, metaglass, 10),
    bryde to with(lead, 100, metaglass, 40, silicon, 40, titanium, 60, thorium, 15),
    sei to with(copper, 300, metaglass, 80, graphite, 80, titanium, 60, plastanium, 20, surgeAlloy, 5),
    omura to with(
        copper, 400, lead, 400, silicon, 100, metaglass, 120,
        graphite, 100, titanium, 120, thorium, 60, phaseFabric, 10, surgeAlloy, 10
    ),

    // Ship Support
    retusa to with(copper, 20, metaglass, 5,titanium, 5),
    oxynoe to with(copper, 30, lead, 40, metaglass, 8,titanium, 8),
    cyerce to with(lead, 100, metaglass, 40, silicon, 40, titanium, 80, thorium, 10),
    aegires to with(copper, 300, metaglass, 80, graphite, 80, titanium, 80, plastanium, 25),
    navanax to with(
        copper, 400, lead, 400, silicon, 100, metaglass, 120,
        graphite, 100, titanium, 80, plastanium, 45, phaseFabric, 5, surgeAlloy, 15
    ),

    // Basic
    alpha to with(copper, 5, lead, 10, silicon, 2, graphite, 3, metaglass, 3),
    beta to with(copper, 8, silicon, 5),
    gamma to with(lead, 10, metaglass, 8, graphite, 8),

    //Erekir Field
    //Tank (focus on mining res)
    stell to with(beryllium, 20, graphite, 15),
    locus to with(beryllium, 50, graphite, 30, tungsten, 15),
    precept to with(beryllium, 100, graphite, 45, tungsten, 25, thorium, 20),
    vanquish to with(beryllium, 300, graphite, 100, tungsten, 80, thorium, 55, oxide, 15),
    conquer to with(
        beryllium, 400, graphite, 400, silicon, 120, tungsten, 120, thorium, 80,
        oxide, 10, carbide, 5
    ),

    //Mech (focus on process res)
    merui to with(beryllium, 25, silicon ,10),
    cleroi to with(beryllium, 50, graphite, 30, silicon ,25, oxide,5),
    anthicus to with(beryllium, 100, graphite, 45, silicon, 50, oxide, 20),
    tecta to with(beryllium, 300, graphite, 100, silicon, 80, oxide, 25, carbide, 5),
    collaris to with(
        beryllium, 400, graphite, 400, silicon, 150, thorium, 120,
        oxide, 35, carbide, 20
    ),

    //Ship (mixed)
    elude to with(graphite  , 25, silicon ,10),
    avert to with(beryllium, 50, tungsten, 30, silicon ,25),
    obviate to with(beryllium, 100, graphite, 45, silicon, 50, thorium, 20),
    quell to with(beryllium, 300, graphite, 150, silicon, 100, thorium, 50, oxide, 25),
    disrupt to with(
        beryllium, 400, graphite, 400, silicon, 130, tungsten, 100, thorium, 80,
        oxide, 25, carbide, 10
    ),

    // erekir core unit(might be killed at certain situation)
    evoke to with(beryllium, 35, graphite, 35, silicon, 20),
    incite to with(beryllium, 40, graphite, 40, tungsten, 20),
    emanate to with(beryllium, 50, graphite, 50, tungsten, 30, thorium, 15),
)

fun handleDrop(unit: Unit, drop: Array<ItemStack>?) {
    if (unit.spawnedByCore || drop == null) return

    //Why: closetEnemyCore = Enemy.closestCore != closest EmeryCore
//    val core = unit.closestEnemyCore() ?: return
    val core = unit.team.data().coreEnemies
        .mapNotNull { it.cores().minByOrNull(unit::dst2) }
        .minByOrNull(unit::dst2) ?: return

    val msg = drop.joinToString(" ") {
        val amount = Random.nextInt(it.amount - it.amount / 2, it.amount + it.amount * 2)
        val accept = core.acceptStack(it.item, amount, null)
        if (accept > 0)
            Call.transferItemTo(
                null, it.item, accept,
                unit.x + Random.nextFloat() * 2, unit.y + Random.nextFloat() * 2, core
            )
        "[accent]+$amount[]${it.item.emoji()}"
    }
    Call.label(msg, drop.size * 0.3f, unit.x + (Random.nextFloat() * 4 - 2f), unit.y + (Random.nextFloat() * 4 - 2f))
}

val tdMode: String? get() = state.rules.tags.get("@TDDrop")
listen<EventType.UnitDestroyEvent> { e ->
    when (tdMode) {
        null, "false", "off" -> return@listen
        "all" -> handleDrop(e.unit, dropMap[e.unit.type])
        else -> if (e.unit.team == state.rules.waveTeam) handleDrop(e.unit, dropMap[e.unit.type])
    }
}