package mapScript.shared

import arc.math.geom.Point2
import cf.wayzer.scriptAgent.thisContextScript
import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.Schematic
import mindustry.game.Schematics
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.world.blocks.storage.CoreBlock

object HexData {
    @Suppress("SpellCheckingInspection")
    val defaultCoreSchema =
        Schematics.readBase64(
            "bXNjaAB4nE2SgY7CIAyGC2yDsXkXH2Tvcq+AkzMmc1tQz/j210JpXDL8hu3/lxYY4FtBs4ZbBLvG1ync4wGO87bvMU2vsCzTEtIlwvCxBW7e1r/43hKYkGY4nFN4XqbfMD+29IbhvmHOtIc1LjCmuIcrfm3X9QH2PofHIyYY5y3FaX3OS3ze4fiRwX7dLa5nDHTPddkCkT3l1DcA/OALihZNq4H6NHnV+HZCVshJXA9VYZC9kfVU+VQGKSsbjVT1lOgp1qO4rGIo9yvnquxH1ORIohap6HVIDbtpaNlDi4cWD80eFJdrNhbJc8W61Jzdqi/3wrRIRii7GYdelvWMZDQs1kNbqtYe9/KuGvDX5zD6d5SML66+5dwRqXgQee5GK3Edxw1ITfb3SJ71OomzUAdjuWsWqZyJavd8Issdb5BqVbaoGCVzJqrddaUGTWSFHPs67m6H5HlaTqbqpFc91Kfn+2eQSp9pr96/Xtx6cevZjeKKDuUOklvvXy9uPGdNZFjZi7IXZS/n8Hyf/wFbjj/q"
        )!!

    var coreSchema: Schematic = defaultCoreSchema
    val extraLoadout = mutableListOf<Data.() -> Unit>()

    data class TeamData(
        val team: Team
    ) {
        val hexes = mutableSetOf<Data>()
    }

    data class Data(
        val id: Int,
        val x: Int, val y: Int
    ) {
        var controller: Team = Team.derelict
            private set
        val coreTile get() = Vars.world.tile(x, y)!!

        fun available(): Boolean {
            if (coreTile.block() is CoreBlock) return false
            return controller == Team.derelict
        }

        fun occupy(team: Team) {
            controller.hexData.hexes.remove(this)

            controller = team
            if (team == Team.derelict) return
            team.hexData.hexes.add(this)
            val core = coreSchema.tiles.find { it.block is CoreBlock }
            if (core == null) {
                thisContextScript().logger.severe("no core tile in Schematic")
                return
            }
            val ox = x - core.x
            val oy = y - core.y
            coreSchema.tiles.forEach { st ->
                val tile = Vars.world.tile(st.x + ox, st.y + oy) ?: return@forEach
                if (tile.block() != Blocks.air) tile.removeNet()
                tile.setNet(st.block, team, st.rotation.toInt())
                st.config?.let { tile.build.configureAny(it) }
            }
        }

        fun giveLoadOut() {
            if (coreTile.block() is CoreBlock)
                coreTile.build.items.add(Vars.state.rules.loadout)
            extraLoadout.forEach { it() }
        }
    }

    val hexDataMap = mutableMapOf<Team, TeamData>()
    val Team.hexData get() = hexDataMap.getOrPut(this) { TeamData(this) }

    lateinit var hexes: List<Data>
    val pos2hex = mutableMapOf<Int, Data>()
    fun init(hex: List<Point2>, coreSchema: Schematic = defaultCoreSchema) {
        this.coreSchema = coreSchema
        hexes = hex.mapIndexed { index, point2 ->
            Data(index, point2.x, point2.y).also {
                pos2hex[point2.pack()] = it
            }
        }
    }

    var stage = 1
    val teams = mutableMapOf<String, Team>()
    fun assignTeam(p: Player, players: Iterable<Player>): Team {
        return when (stage) {
            1 -> {
                teams[p.uuid()]
                    ?.takeIf { it.active() && players.none { pp -> pp.team() == it } }
                    ?.let { return it }

                if (Vars.state.teams.active.any { it.team.hexData.hexes.size >= 4 }) {
                    broadcast("[red][HEX PVP][yellow]巨头出现，进入第二阶段".with())
                    stage = 2
                    return assignTeam(p, players)
                }

                val hex = hexes.filter { it.available() }
                    .randomOrNull() ?: let {
                    broadcast("[red][HEX PVP][yellow]所有区块分配完毕, 进入第二阶段".with())
                    stage = 2
                    return assignTeam(p, players)
                }
                val team = Team.all.first {
                    it.id > 6 && !it.active() && players.none { pp -> pp.team() == it }
                }
                hex.occupy(team)
                hex.giveLoadOut()
                teams[p.uuid()] = team
                team
            }

            2 -> {
                teams[p.uuid()]?.takeIf { it.data().hasCore() }
                    ?.let { return it }
                val team = Vars.state.teams.active.select { it.hasCore() }.map { it.team }
                    .minByOrNull {
                        players.count { pp -> pp.team() == it } * 3 + it.hexData.hexes.size
                    }
                    ?: Team.get(255)
                teams[p.uuid()] = team
                team
            }

            else -> error("Unsupport stage: $stage")
        }
    }


    fun reset() {
        extraLoadout.clear()
        hexDataMap.clear()
        teams.clear()
        pos2hex.clear()
        hexes = emptyList()
        stage = 1
    }
}