package coreLibrary

import coreLibrary.lib.PlaceHold.registeredVars
import coreLibrary.lib.util.menu

data class VarInfo(val script: ScriptInfo, val key: String, val desc: String)

onEnable {
    Commands.controlCommand += CommandInfo(this, "vars", "列出注册的所有模板变量") {
        usage = "[-v] [page]"
        permission = "scriptAgent.vars"
        body {
            val detail = checkArg("-v")
            val page = arg.firstOrNull()?.toIntOrNull() ?: 1
            val all = mutableListOf<VarInfo>()
            ScriptManager.allScripts.values.sortedBy { it.id }.forEach { script ->
                script.inst?.registeredVars?.mapTo(all) { (key, desc) ->
                    VarInfo(script, key, desc)
                }
            }
            returnReply(menu("模板变量", all, page, 15) {
                "[green]{key} [blue]{desc} [purple]{from}".with(
                    "key" to it.key, "desc" to it.desc,
                    "from" to (if (detail) it.script.id else "")
                )
            })
        }
    }
}