package mapScript.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.depends
import cf.wayzer.scriptAgent.import
import cf.wayzer.scriptAgent.util.DSLBuilder
import mindustry.mod.data.DataAsset

@ScriptDsl
fun Script.modeIntroduce(mode: String, introduce: String) {
    onEnable {
        depends("wayzer/map/mapInfo")?.import<(String, String) -> Unit>("addModeIntroduce")
            ?.invoke(mode, introduce)
    }
}

@ScriptDsl
@Deprecated("use mapAssets")
var Script.mapPatches by DSLBuilder.dataKey<List<String>>()
@ScriptDsl
var Script.mapAssets by DSLBuilder.dataKey<List<DataAsset>>()