package coreStandalone

import kotlin.system.exitProcess

command("stop", "结束程序") {
    body {
        reply("Bye!".with())
        ScriptManager.disableAll()
        exitProcess(0)
    }
}