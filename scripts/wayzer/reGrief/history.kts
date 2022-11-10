package wayzer.reGrief

import arc.graphics.Color
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.type.Item
import mindustry.world.Block
import mindustry.world.blocks.storage.CoreBlock
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
sealed class Log(val uid: String?, private val desc: () -> String) {
    val time = Date()

    class Place(uid: String?, val type: Block) : Log(uid, {
        "放置了方块${type.name}"
    })

    class Break(uid: String?) : Log(uid, {
        "拆除了方块"
    })

    class Config(uid: String?, val value: String) : Log(uid, {
        "修改了属性: $value"
    })

    class Deposit(uid: String?, val item: Item, val amount: Int) : Log(uid, {
        "往里面丢了${amount}个${item.name}"
    })

    class Destroy : Log(null, {
        "拆毁了方块"
    })

    class PickUp(uid: String?) : Log(uid, {
        "拾起了方块"
    })

    //TODO no event
//    class PickDown(uid: String?, val type: Block) : Log(uid, {
//        "放下了方块: ${type.name}"
//    })

    fun descLog(descPrefix: String = ""): PlaceHoldString {
        val desc = descPrefix + desc()
        return if (uid == null) {
            "[red]{time:HH:mm:ss}[]-[yellow]未知单位[white]{desc}".with("time" to time, "desc" to desc)
        } else {
            val info = netServer.admins.getInfo(uid)
            "[red]{time:HH:mm:ss}[]-[yellow]{info.name}[yellow]({info.shortID})[white]{desc}"
                .with("time" to time, "desc" to desc, "info" to info)
        }
    }
}

PermissionApi.registerDefault("wayzer.ext.history")
val historyLimit by config.key(10, "单格最长日记记录")
lateinit var logs: Array<Array<List<Log>>>

//初始化
fun initData() {
    logs = Array(world.width()) {
        Array(world.height()) {
            emptyList()
        }
    }
}
onEnable {
    if (net.server())
        initData()
}
listen<EventType.WorldLoadEvent> {
    initData()
}

//记录
fun log(x: Int, y: Int, log: Log) {
    if (historyLimit <= 0) return
    if (logs[x][y].isEmpty()) logs[x][y] = LinkedList(listOf(log))
    else with(logs[x][y] as LinkedList) {
        while (size >= historyLimit)
            remove()
        add(log)
    }
}
listen<EventType.BlockBuildEndEvent> {
    val player = it.unit?.player ?: return@listen
    if (it.breaking)
        log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Break(player?.uuid()))
    else
        log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Place(player?.uuid(), it.tile.block()))
}
listen<EventType.ConfigEvent> {
    val log = Log.Config(it.player?.uuid(), it.value?.toString() ?: "null")
    log(it.tile.tileX(), it.tile.tileY(), log)
}
listen<EventType.DepositEvent> {
    log(it.tile.tileX(), it.tile.tileY(), Log.Deposit(it.player?.uuid(), it.item, it.amount))
}
listen<EventType.BlockDestroyEvent> {
    log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Destroy())
}
listen<EventType.PickupEvent> {
    val build = it.build ?: return@listen
    //As the build has removed when pickup, use tileOn instead
    val tile = build.tileOn()
    log(tile.centerX(), tile.centerY(), Log.PickUp(it.carrier.player?.uuid()))
}

fun Player.showLog(xf: Float, yf: Float) {
    val x = xf.toInt() / 8
    val y = yf.toInt() / 8
    if (x < 0 || x >= world.width()) return
    if (y < 0 || y >= world.height()) return
    val logs = logs[x][y]
    if (logs.isEmpty()) Call.label(
        con,
        "[yellow]位置({x},{y})无记录".with("x" to x, "y" to y).toPlayer(this),
        3f, xf, yf
    )
    else {
        val list = logs.map { log -> log.descLog() }
        Call.label(
            con,
            "====[gold]操作记录({x},{y})[]====\n{list:\n}"
                .with("x" to x, "y" to y, "list" to list)
                .toPlayer(this),
            10f, xf, yf
        )
    }
}

//查询
val enabledPlayer = mutableSetOf<String>()
command("history", "开关查询模式") {
    permission = "wayzer.ext.history"
    usage = "[core(查询核心)]"
    aliases = listOf("历史")
    body {
        when (arg.getOrElse(0) { "" }) {
            "core" -> returnReply(
                "[green]核心破坏周边情况:\n{list:\n}".with("list" to lastCoreLog)
            )
        }
        if (player == null) returnReply("[red]控制台仅可查询核心破坏记录".with())
        if (player!!.uuid() in enabledPlayer) {
            enabledPlayer.remove(player!!.uuid())
            reply("[green]关闭查询模式".with())
        } else {
            enabledPlayer.add(player!!.uuid())
            reply("[green]开启查询模式,点击方块查询历史".with())
        }
    }
}

listen<EventType.TapEvent> {
    val p = it.player
    if (p.uuid() !in enabledPlayer) return@listen
    Call.effect(p.con, Fx.placeBlock, it.tile.worldx(), it.tile.worldy(), 0.5f, Color.green)
    p.showLog(it.tile.worldx(), it.tile.worldy())
}

// 自动保留破坏核心的可疑行为
var lastCoreLog = emptyList<PlaceHoldString>()
var lastTime = 0L
val dangerBlock = arrayOf(
    Blocks.thoriumReactor,
    Blocks.liquidTank, Blocks.liquidRouter, Blocks.bridgeConduit, Blocks.phaseConduit,
    Blocks.conduit, Blocks.platedConduit, Blocks.pulseConduit
)

listen<EventType.BlockDestroyEvent> { event ->
    if (event.tile.block() is CoreBlock) {
        if (System.currentTimeMillis() - lastTime > 5000) { //防止核心连环爆炸,仅记录第一个被炸核心
            val list = mutableListOf<PlaceHoldString>()
            for (x in event.tile.x.let { it - 10..it + 10 })
                for (y in event.tile.y.let { it - 10..it + 10 })
                    logs.getOrNull(x)?.getOrNull(y)?.lastOrNull { it is Log.Place }?.let { log ->
                        if (log is Log.Place && log.type in dangerBlock)
                            list.add(log.descLog("在距离核心(${x - event.tile.x},${y - event.tile.y})的位置"))
                    }
            lastCoreLog = list
        }
        lastTime = System.currentTimeMillis()
    }
}

