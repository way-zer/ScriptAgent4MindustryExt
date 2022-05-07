package mapScript

import arc.math.Mathf
import arc.math.geom.Geometry
import mindustry.content.Blocks.*
import mindustry.content.Weathers
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.production.Fracker
import mindustry.world.blocks.production.Pump
import mindustry.world.blocks.production.SolidPump
import kotlin.random.Random

/**动态地貌脚本
 * Contributor(s): BlackDeluxeCat
 * */
//自然转化
val sporeGrowRate = 0.3     //孢子生油随机成熟：普通孢子->茂密孢子->孢子灌木丛->石油
val sporeGrowList = mapOf(moss to sporeMoss, sporeMoss to tar)

val wetRate = 0.4           //土地的邻近传播：水源：盐碱地->泥土->草地->泥泞；草地/泥泞源：盐碱地->泥土->草地
val wetSourcesList =
    listOf(deepwater, water, taintedWater, deepTaintedWater, darksandTaintedWater, sandWater, darksandWater)
val wetList = mapOf(salt to dirt, dirt to grass, grass to mud)
val grassSourcesList = listOf(grass, mud)
val grassList = mapOf(salt to dirt, dirt to grass)

val lavaGushRate = 0.2    //岩浆涌出：玄武岩->热地->熔岩地->岩浆
val lavaGushList = mapOf(basalt to hotrock, hotrock to magmarock, magmarock to slag)
val cryoGushRate = 0.2      //冷液渗出：英安岩->冷液
val cryoGushList = mapOf(dacite to cryofluid)
val waterGushRate = 0.4      //水渗出：石坑->水
val waterGushList = mapOf(craters to water)

//人类影响
val pumpDecayRate = 0.2        //液泵/地热元件损耗地貌：
//油->孢子地； 深水/污水->水->英安岩；岩浆->熔岩地->热地->玄武岩；冷液->英安岩
val pumpDecayList = mapOf(
    tar to moss,
    slag to hotrock,
    cryofluid to dacite,
    water to craters,
    darksandWater to craters,
    sandWater to craters,
    deepwater to water,
    taintedWater to water,
    darksandTaintedWater to water
)
val thermalDecaySources = listOf(siliconCrucible, thermalGenerator)
val thermalDecayList = mapOf(slag to magmarock, magmarock to hotrock, hotrock to basalt)

val extractDecayRate = 0.5      //抽水，抽油损耗地貌：
// 油：黑沙->黄沙，页岩->盐碱地；
// 水：冰->冰雪->雪->泥泞->泥土->盐碱地，草地->泥土
val waterExtractDecayList = mapOf(
    ice to iceSnow,
    iceSnow to snow,
    snow to mud,
    mud to dirt,
    grass to dirt,
    dirt to salt
)
val oilExtractDecayList = mapOf(darksand to sand, shale to salt)

//天气转化
val rainWetRate = 0.8           //降雨湿润土地：盐碱地->泥土->草地->泥泞，石坑->水
val rainWetList = mapOf(salt to dirt, dirt to grass, grass to mud, craters to water)
val sporeRainGrowRate = 0.5     //孢子雨催熟孢子地：普通孢子->茂密孢子
val sporeRainGrowList = mapOf(moss to sporeMoss)
val sandstormRecoverRate = 0.7  //沙暴恢复沙地：黄沙->黑沙
val sandstormRecoverList = mapOf(sand to darksand)
val sandstormDryRate = 0.5          //沙暴蒸发水：冰->冰雪->雪->泥泞->草地->泥土
val sandstormDryList = mapOf(ice to iceSnow, iceSnow to snow, snow to mud, mud to grass, grass to dirt)
val snowSnowRate = 0.8          //降雪堆积：草地/泥土/泥泞->雪->冰雪->冰
val snowSnowList = mapOf(grass to snow, dirt to snow, mud to snow, snow to iceSnow, iceSnow to ice)

fun Tile.transform(rate: Double, map: Map<Block, Block>) {
    if (!Mathf.chance(rate)) return
    map[floor()]?.let { setFloorNet(it, overlay()) }
}

//邻近转化
fun Tile.spread(rate: Double, sources: List<Block>, map: Map<Block, Block>) {
    if (!sources.contains(floor())) return
    for (p in Geometry.d4) {
        world.tiles.get(x + p.x, y + p.y)?.transform(rate, map)
    }
}

listen(EventType.Trigger.update) {
    if (state.isPaused) return@listen
    repeat(8) {
        val tile = world.tiles.getn(Random.nextInt(world.width()), Random.nextInt(world.height()))
        //自然转化
        tile.transform(sporeGrowRate, sporeGrowList)
        tile.spread(wetRate, wetSourcesList, wetList)
        tile.spread(wetRate, grassSourcesList, grassList)
        tile.transform(lavaGushRate, lavaGushList)
        tile.transform(cryoGushRate, cryoGushList)
        //人类影响
        when {
            tile.block() is Fracker -> tile.transform(extractDecayRate, oilExtractDecayList)
            tile.block() is SolidPump -> tile.transform(extractDecayRate, waterExtractDecayList)
            tile.block() is Pump -> tile.transform(pumpDecayRate, pumpDecayList)
            thermalDecaySources.contains(tile.block()) -> tile.transform(pumpDecayRate, thermalDecayList)
        }
        //天气转化
        if (Weathers.rain.isActive) tile.transform(rainWetRate, rainWetList)
        if (Weathers.sporestorm.isActive) tile.transform(sporeRainGrowRate, sporeRainGrowList)
        if (Weathers.sandstorm.isActive) {
            tile.transform(sandstormRecoverRate, sandstormRecoverList)
            tile.transform(sandstormDryRate, sandstormDryList)
        }
        if (Weathers.snow.isActive) tile.transform(snowSnowRate, snowSnowList)
    }
}