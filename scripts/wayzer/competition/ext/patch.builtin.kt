package wayzer.competition.ext

import arc.struct.Seq
import cf.wayzer.scriptAgent.define.Script
import mindustry.Vars.state
import mindustry.content.Blocks.*
import mindustry.content.Planets.*
import mindustry.content.UnitTypes.*
import mindustry.game.Rules
import mindustry.maps.Map
import mindustry.maps.filters.ClearFilter
import mindustry.maps.filters.GenerateFilter

object BuiltinPatch {
    private fun applyRules(body: Rules.() -> Unit) {
        body(state.rules)
    }
    private fun applyMap(body: Map.() -> Unit) {
        body(state.map)
    }
    fun onLoad(script: Script) = with(script) {
        mapPatch("错误投放", "初始资源清零", cond={ (loadout?.size ?: 0) != 0 }) {
            applyRules {
                loadout.clear()
            }
        }
        mapPatch("单人行动","所有空辅被禁用,建造速度变为二倍", env=setOf(serpulo, sun), cond={ !bannedUnits.contains(mono) }) {
            applyRules {
                bannedUnits.addAll(mono, poly, mega, oct, quad)
                buildSpeedMultiplier *= 2
            }
        }
        mapPatch("永久破坏","建筑无法被重建", cond={ ghostBlocks }) {
            applyRules {
                ghostBlocks = false
            }
        }
        mapPatch("失重环境","单位坠毁伤害变为一半", env=setOf(serpulo)) {
            applyRules {
                unitCrashDamageMultiplier *= 0.5f
            }
        }
        mapPatch("环保卫士","太阳能板发电量变为三倍", env=setOf(serpulo, sun), cond={ solarMultiplier>=1 && !bannedBlocks.contains(solarPanel) }) {
            applyRules {
                solarMultiplier *= 3
            }
        }
        mapPatch("核弹危机","反应堆爆炸开启", env=setOf(serpulo), cond={ !reactorExplosions && !bannedBlocks.contains(thoriumReactor)}) {
            applyRules {
                reactorExplosions = true
            }
        }
        mapPatch("智械危机","小，中逻辑块被禁用", env=setOf(serpulo, sun), cond={ !bannedBlocks.contains(microProcessor)} ) {
            applyRules {
                bannedBlocks.addAll(microProcessor, logicProcessor)
            }
        }
        mapPatch("失落科技","合金坩埚被禁用", env=setOf(erekir), cond={ !bannedBlocks.contains(surgeCrucible) }) {
            applyRules {
                bannedBlocks.add(surgeCrucible)
            }
        }
        mapPatch("易碎核心", "核心占领关闭", cond={ coreCapture }) {
            applyRules {
                coreCapture = false
            }
        }
        mapPatch("时间凝固", "超速投影被禁用", env=setOf(serpulo, sun), cond={ !bannedBlocks.contains(overdriveProjector) || !bannedBlocks.contains(overdriveDome) }) {
            applyRules {
                bannedBlocks.addAll(overdriveProjector, overdriveDome)
            }
        }
        mapPatch("石油枯竭", "所有石油变为页岩地", env=setOf(serpulo, sun), cond={ !bannedBlocks.contains(oilExtractor) && !bannedBlocks.contains(plastaniumCompressor) }) {
            applyMap {
                val filters : Seq<GenerateFilter> = filters()
                filters.addAll(
                        ClearFilter().apply{ target = tar; replace = shale },
                )
                tags.put("genfilters", mindustry.io.JsonIO.write(filters))
            }
        }
        mapPatch("地热冷却","所有地热类型地板变为热石头", env=setOf(serpulo, sun), cond={ !bannedBlocks.contains(thermalGenerator) }) {
            applyMap {
                val filters : Seq<GenerateFilter> = filters()
                filters.addAll(
                        ClearFilter().apply{ target = slag; replace = hotrock },
                        ClearFilter().apply{ target = magmarock; replace = hotrock }
                )
                tags.put("genfilters", mindustry.io.JsonIO.write(filters))
            }
        }
        //平衡性较差，暂不启用
        mapPatch("王牌空战","所有单位均可飞行", env=setOf(), cond = { false }) {
        }
        mapPatch("短兵相接", "核心保护区缩小至1/2", env=setOf(), cond={ !polygonCoreProtection && false }) {
            applyRules {
                enemyCoreBuildRadius /= 2
            }
        }
    }
}