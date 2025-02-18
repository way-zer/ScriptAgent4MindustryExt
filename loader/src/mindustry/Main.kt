package cf.wayzer.scriptAgent.mindustry

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.scriptAgent.*
import cf.wayzer.scriptAgent.define.LoaderApi
import cf.wayzer.scriptAgent.util.CommonMain
import kotlinx.coroutines.runBlocking
import mindustry.Vars
import mindustry.mod.Plugin

@OptIn(LoaderApi::class)
class Main(private val loader: Plugin) : Plugin(), CommonMain {
    override fun getConfig(): Fi = loader.config

    override fun registerClientCommands(handler: CommandHandler) {
        //call after init(), too late
//        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    override fun init() {
        Config.version = Vars.mods.getMod(loader.javaClass).meta.version
        Config.rootDir = Vars.dataDirectory.child("scripts").file()
        Config.clientCommands = Vars.netServer?.clientCommands ?: CommandHandler("/")
        if (!Vars.headless) Config.serverCommands = CommandHandler("")

        bootstrap()
        Core.app.addListener(object : ApplicationListener {
            override fun pause() {
                if (Vars.headless)
                    exit()
            }

            override fun exit() = runBlocking {
                ScriptManager.disableAll()
            }
        })
    }

    override fun displayInfo(foundMain: Boolean) {
        Log.info("&y===========================")
        Log.info("&lm&fb     ScriptAgent &b${Config.version}")
        Log.info("&b           By &cWayZer    ")
        Log.info("&b插件官网: https://git.io/SA4Mindustry")
        Log.info("&bQQ交流群: 1033116078")
        val all = ScriptRegistry.allScripts { true }
        Log.info(
            "&b共找到${all.size}脚本,加载成功${all.count { it.scriptState.loaded }},启用成功${all.count { it.scriptState.enabled }},出错${all.count { it.failReason != null }}"
        )
        if (!foundMain)
            Log.warn("&c未找到启动脚本(${Config.mainScript}),请下载安装脚本包,以发挥本插件功能")
        Log.info("&y===========================")
    }
}