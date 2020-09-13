package wayzer.ext.reGrief

import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.type.Item
import mindustry.world.Block
import mindustry.world.blocks.storage.CoreBlock
import java.time.Instant
import java.util.*

sealed class Log(val uid: String, val time: Instant) {
    class Place(uid: String, time: Instant, val type: Block) : Log(uid, time)
    class Break(uid: String, time: Instant) : Log(uid, time)
    class Config(uid: String, time: Instant, val value: Int) : Log(uid, time)
    class Deposit(uid: String, time: Instant, val item: Item, val amount: Int) : Log(uid, time)
}

val historyLimit by config.key(10, "单格最长日记记录")
lateinit var logs: Array<Array<List<Log>>>

//初始化
fun initData() {
    logs = Array(world.width()) {
        Array(world.height()) {
            emptyList<Log>()
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
    if (it.breaking)
        log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Break(it.player.uuid, Instant.now()))
    else if (it.player != null)
        log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Place(it.player.uuid, Instant.now(), it.tile.block()))
}
listen<EventType.TapConfigEvent> {
    if (it.player == null) return@listen
    log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Config(it.player.uuid, Instant.now(), it.value))
}
listen<EventType.DepositEvent> {
    log(it.tile.x.toInt(), it.tile.y.toInt(), Log.Deposit(it.player.uuid, Instant.now(), it.item, it.amount))
}

//查询
val enabledPlayer = mutableSetOf<String>()
command("history", "开关查询模式", {
    permission = "wayzer.ext.history"
    usage = "[core(查询核心)]"
    aliases = listOf("历史")
}) {
    if (arg.getOrElse(0) { "" }.contains("core")) returnReply(
            "[green]核心破坏周边情况:\n{list}".with("list" to lastCoreLog)
    )
    if (player == null) returnReply("[red]控制台仅可查询核心破坏记录".with())
    if (player!!.uuid in enabledPlayer) {
        enabledPlayer.remove(player!!.uuid)
        reply("[green]关闭查询模式".with())
    } else {
        enabledPlayer.add(player!!.uuid)
        reply("[green]开启查询模式,点击方块查询历史".with())
    }
}
listen<EventType.TapEvent> {
    if (it.player.uuid !in enabledPlayer) return@listen
    val x = it.tile.x.toInt()
    val y = it.tile.y.toInt()
    val logs = logs[x][y]
    if (logs.isEmpty()) Call.onLabel(it.player.con, "[yellow]位置($x,$y)无记录", 5f, it.tile.getX(), it.tile.getY())
    else {
        val list = logs.map { log ->
            "[red]{time:HH:mm:ss}[]-[yellow]{info.name}[yellow]({info.shortID})[white]{desc}\n".with(
                    "time" to Date.from(log.time), "info" to netServer.admins.getInfo(log.uid), "desc" to when (log) {
                is Log.Place -> "放置了方块${log.type.name}"
                is Log.Break -> "拆除了方块"
                is Log.Config -> "修改了属性: ${log.value}"
                is Log.Deposit -> "往里面丢了${log.amount}个${log.item.name}"
            })
        }
        Call.onLabel(it.player.con, "====[gold]操作记录($x,$y)[]====\n{list}".with("list" to list).toString(), 15f, it.tile.getX(), it.tile.getY())
    }
}

// 自动保留破坏核心的可疑行为
var lastCoreLog = emptyList<String>()
var lastTime = 0L
val dangerBlock = arrayOf(
        Blocks.thoriumReactor,
        Blocks.liquidTank, Blocks.liquidRouter, Blocks.bridgeConduit, Blocks.phaseConduit,
        Blocks.conduit, Blocks.platedConduit, Blocks.pulseConduit)
listen<EventType.BlockDestroyEvent> { event ->
    if (event.tile.block() is CoreBlock) {
        if (System.currentTimeMillis() - lastTime < 5000) { //防止核心连环爆炸,仅记录第一个被炸核心
            val list = mutableListOf<String>()
            for (x in event.tile.x.let { it - 10..it + 10 })
                for (y in event.tile.y.let { it - 10..it + 10 })
                    logs.getOrNull(x)?.getOrNull(y)?.lastOrNull { it is Log.Place }?.let { log ->
                        if (log is Log.Place && log.type in dangerBlock)
                            list.add("[red]{time:HH:mm:ss}[]-[yellow]{info.name}[yellow]({info.shortID})[white]{desc}\n".with(
                                    "time" to Date.from(log.time), "info" to netServer.admins.getInfo(log.uid),
                                    "desc" to "在距离核心(${x - event.tile.x},${y - event.tile.y})的位置放置了${log.type.name}"
                            ).toString())
                    }
            lastCoreLog = list
        }
        lastTime = System.currentTimeMillis()
    }
}

