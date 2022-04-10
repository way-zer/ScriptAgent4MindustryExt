package mapScript.lib

import cf.wayzer.scriptAgent.ScriptManager
import cf.wayzer.scriptAgent.define.IScript
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptState

//for old ScriptAgent version
fun ScriptManager.newLoadScript(
    script: IScript,
    force: Boolean = false,
    enable: Boolean = true,
    children: Boolean = true
): Script? {
    val newForce = force && script.scriptState != ScriptState.ToEnable
    return loadScript(script, newForce, enable, children)
}