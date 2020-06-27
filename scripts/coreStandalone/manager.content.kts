import cf.wayzer.script_agent.Config
import kotlin.system.exitProcess

RootCommands.addSub(Command(this, "stop", "结束程序") {
    sendMessage("Bye!".with())
    Config.inst.disableAll()
    exitProcess(0)
})