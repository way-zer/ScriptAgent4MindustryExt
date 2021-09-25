package cf.wayzer

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.ConfigExt.clientCommands
import cf.wayzer.ConfigExt.serverCommands
import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptAgent
import cf.wayzer.scriptAgent.ScriptManager
import cf.wayzer.scriptAgent.define.LoaderApi
import cf.wayzer.scriptAgent.emit
import cf.wayzer.scriptAgent.events.FinishLoadEvent
import mindustry.Vars
import mindustry.plugin.Plugin

class ScriptAgent4Mindustry : Plugin() {
    init {
        if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
            System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n"
            )
        @OptIn(LoaderApi::class)
        ScriptAgent.load()
    }

    override fun registerClientCommands(handler: CommandHandler) {
        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    @OptIn(LoaderApi::class)
    override fun init() {
        val dir = Vars.dataDirectory.child("scripts").file()
        Config.rootDir = dir
        System.getenv("SAMain")?.let { id ->
            Log.info("发现环境变量SAMain=$id")
            val script = ScriptManager.getScriptNullable(id)
                ?: error("未找到脚本$id")
            Log.info("发现脚本$id,开始加载")
            ScriptManager.loadScript(script, enable = false, children = false)
            FinishLoadEvent(false).emit()
            ScriptManager.enableAll()
        } ?: let {
            @OptIn(LoaderApi::class)
            ScriptManager.loadDir(dir)
        }
        Core.app.addListener(object : ApplicationListener {
            override fun pause() {
                ScriptManager.disableAll()
            }
        })
        Log.info("&y===========================")
        Log.info("&lm&fb     ScriptAgent          ")
        Log.info("&b           By &cWayZer    ")
        Log.info("&b插件官网: https://git.io/SA4Mindustry")
        Log.info("&bQQ交流群: 1033116078")
        if (dir.listFiles()?.isEmpty() != false)
            Log.warn("未在config/scripts下发现脚本,请下载安装脚本包,以发挥本插件功能")
        Log.info("&y===========================")
    }
}