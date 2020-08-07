package coreStandalone

import cf.wayzer.script_agent.ScriptManager
import kotlin.system.exitProcess

command("stop", "结束程序") {
    reply("Bye!".with())
    ScriptManager.disableAll()
    exitProcess(0)
}