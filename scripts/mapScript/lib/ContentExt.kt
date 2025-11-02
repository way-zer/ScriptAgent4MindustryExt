package mapScript.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.depends
import cf.wayzer.scriptAgent.import
import cf.wayzer.scriptAgent.util.DSLBuilder

@ScriptDsl
fun Script.modeIntroduce(mode: String, introduce: String) {
    onEnable {
        depends("wayzer/map/mapInfo")?.import<(String, String) -> Unit>("addModeIntroduce")
            ?.invoke(mode, introduce)
    }
}

@ScriptDsl
var Script.mapScriptController by DSLBuilder.dataKey<Boolean>()

@ScriptDsl
var Script.mapPatches by DSLBuilder.dataKey<List<String>>()