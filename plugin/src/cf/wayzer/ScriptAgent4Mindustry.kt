package cf.wayzer

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import cf.wayzer.ConfigExt.clientCommands
import cf.wayzer.ConfigExt.serverCommands
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptAgent
import cf.wayzer.script_agent.ScriptManager
import mindustry.Vars
import mindustry.mod.Plugin

class ScriptAgent4Mindustry: Plugin() {
    init {
        if(System.getProperty("java.util.logging.SimpleFormatter.format")==null)
            System.setProperty("java.util.logging.SimpleFormatter.format","[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n")
        ScriptAgent.load()
    }
    override fun registerClientCommands(handler: CommandHandler) {
        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    override fun init() {
        ScriptManager.loadDir(Vars.dataDirectory.child("scripts").file())
        Core.app.addListener(object :ApplicationListener{
            override fun pause() {
                ScriptManager.disableAll()
            }
        })
    }
}