package main

import arc.Core
import arc.struct.IntSeq
import arc.util.Time
import arc.util.io.ReusableByteOutStream
import arc.util.io.Writes
import coreLibrary.lib.config
import coreLibrary.lib.util.reflectDelegate
import coreMindustry.lib.game
import coreMindustry.lib.listen
import mindustry.Vars.state
import mindustry.Vars.universe
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.logic.GlobalVars
import mindustry.world.modules.ItemModule
import java.io.DataOutputStream
import kotlin.math.pow

//write with v126.2
name = "同步优化"

val bandwidth by config.key(10240, "期望宽带，单位Kbps")
val maxPrePlayer by config.key(80.0, "玩家最大流量，单位KB/s")

val syncStream = ReusableByteOutStream()
val dataStream = DataOutputStream(syncStream)
val ItemModule.items: IntArray by reflectDelegate()

fun sendState(player: Player) {
    syncStream.reset()
    state.teams.present.select { it.cores.size > 0 }.run {
        dataStream.writeByte(size)
        val dataWrites = Writes.get(dataStream)
        forEach { team ->
            val core = team.cores.first()
            dataStream.writeByte(team.team.id)
            if (!state.rules.pvp || team.team == player.team() || player.team() == Team.get(255))
                core.items.write(dataWrites)
            else {
                val items = core.items.items
                dataStream.writeShort(items.count { it > 0 })
                for (i in items.indices) {
                    if (items[i] > 0) {
                        dataStream.writeShort(i)
                        dataStream.writeInt(items[i].coerceAtMost(100))
                    }
                }
            }
        }
    }
    dataStream.close()

    val tps = Core.graphics.framesPerSecond.coerceAtMost(255).toByte()
    with(state) {
        Call.stateSnapshot(
            player.con, wavetime, wave, enemies, gameOver/*serverPaused*/, gameOver, universe.seconds(),
            tps, GlobalVars.rand.seed0, GlobalVars.rand.seed1, syncStream.toByteArray()
        )
    }
    syncStream.reset()
}

var lastWarn = -1
var lastDrop = -1

fun sendSync(player: Player) {
    sendState(player)
    var sent = 0
    fun trySend(last: Boolean) {
        if ((!last && syncStream.size() <= 800) || sent == 0) return
        dataStream.close()
        if (syncStream.size() > 2000 && syncStream.size() != lastWarn) {
            if (lastDrop != syncStream.size())
                logger.warning("Too Big Packet ${syncStream.size()}B, Drop it")
            lastDrop = syncStream.size()
        } else {
            Call.entitySnapshot(player.con, sent.toShort(), syncStream.toByteArray())
        }
        sent = 0
        syncStream.reset()
    }

    val hiddenIds = IntSeq()
    Groups.sync.sortedBy { it::class.java.canonicalName }.forEach {
        val before = syncStream.size()
        if (it.isSyncHidden(player)) {
            hiddenIds.add(it.id())
            return@forEach
        }
        dataStream.writeInt(it.id())
        dataStream.writeByte(it.classId())
        it.writeSync(Writes.get(dataStream))
        sent++
        trySend(false)
        if (syncStream.size() >= 1500 && syncStream.size() != lastWarn) {
            logger.warning("Big packet ${syncStream.size()}")
            logger.warning("Last entity ${it.javaClass.canonicalName}, use ${syncStream.size() - before} bytes")
            lastWarn = syncStream.size()
        }
    }
    trySend(true)
    if (hiddenIds.size > 0)
        Call.hiddenSnapshot(player.con, hiddenIds)
}

val hackOffset = 1_000_000 //1000s to disable sync of origin
listen<EventType.PlayerJoin> {
    it.player.con.syncTime += hackOffset
}

listen(EventType.Trigger.update) {
    val syncInterval = let {

        val players = Groups.player.size().toDouble()
        val units = Groups.sync.size().toDouble()

        val bandwidthPerPlayer = (bandwidth / players.pow(1.1)).coerceAtMost(maxPrePlayer)
        val syncPerS = (bandwidthPerPlayer / 0.1) / units.pow(0.95)
        1000 / syncPerS.coerceAtMost(5.0)
    }
    Groups.player.forEach { p ->
        val con = p.con
        if (con == null || !con.isConnected) return@forEach
        if (Time.timeSinceMillis(con.syncTime - hackOffset) < syncInterval) return@forEach
        con.syncTime = hackOffset + Time.millis()
        try {
            sendSync(p)
            p.con.snapshotsSent++
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

onEnable {
    launch(Dispatchers.game) {
        Groups.player.forEach {
            if (Time.timeSinceMillis(it.con.syncTime) > 0)//normal
                it.con.syncTime += hackOffset
        }
    }
}

onDisable {
    launch(Dispatchers.game) {
        Groups.player.forEach {
            it.con.syncTime -= hackOffset
        }
    }
}