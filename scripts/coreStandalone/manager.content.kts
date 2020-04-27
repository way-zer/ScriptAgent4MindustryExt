import cf.wayzer.script_agent.Config
import kotlin.system.exitProcess

RootCommands.addSub(Command(this, "stop", "结束程序") {
    Config.inst.disableAll()
    sendMessage("Bye!".with())
    exitProcess(0)
})