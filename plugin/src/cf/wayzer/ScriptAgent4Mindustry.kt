package cf.wayzer

import arc.ApplicationListener
import arc.Core
import arc.util.ColorCodes
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.ConfigExt.clientCommands
import cf.wayzer.ConfigExt.serverCommands
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptAgent
import cf.wayzer.script_agent.ScriptManager
import mindustry.Vars
import mindustry.plugin.Plugin

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
        val dir = Vars.dataDirectory.child("scripts").file()
        ScriptManager.loadDir(dir)
        Core.app.addListener(object : ApplicationListener {
            override fun pause() {
                ScriptManager.disableAll()
            }
        })
        Log.info("&y===========================")
        Log.info("&lm&fb     ScriptAgent          ")
        Log.info("&b           By &cWayZer    ")
        Log.info("&b插件官网: http://git.io/SA4Mindustry")
        Log.info("&bQQ交流群: 1033116078")
        if (dir.listFiles()?.isEmpty() != false)
            Log.warn("未在config/scripts下发现脚本,请下载安装脚本包,以发挥本插件功能")
        Log.info("&y===========================")
    }
}