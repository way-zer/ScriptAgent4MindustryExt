package cf.wayzer.scriptAgent.mindustry

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.scriptAgent.*
import cf.wayzer.scriptAgent.define.LoaderApi
import kotlinx.coroutines.runBlocking
import mindustry.Vars
import mindustry.mod.Plugin
import java.io.File

@OptIn(LoaderApi::class)
class Main(private val loader: Plugin) : Plugin() {
    override fun getConfig(): Fi = loader.config

    override fun registerClientCommands(handler: CommandHandler) {
        //after init() is too late
//        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    override fun init() {
        val defaultMain = "main/bootStrap"
        val version = Vars.mods.getMod(loader.javaClass).meta.version
        val main = System.getenv("SAMain") ?: defaultMain
        Log.info("SAMain=$main")

        Config.version = version
        Config.mainScript = main
        Config.rootDir = Vars.dataDirectory.child("scripts").file()
        Config.clientCommands = Vars.netServer?.clientCommands ?: CommandHandler("/")
        if (!Vars.headless) {
            Config.serverCommands = CommandHandler("")
        }

        tryExtract("/res/$defaultMain.kts", Config.rootDir.resolve("$defaultMain.kts"))
        tryExtract("/res/$defaultMain.ktc", Config.cacheFile(defaultMain, false))
        ScriptRegistry.scanRoot()

        val script = ScriptRegistry.findScriptInfo(main)
        if (script != null) runBlocking {
            ScriptManager.transaction {
                add(script)
                load();enable()
            }
            Core.app.addListener(object : ApplicationListener {
                override fun pause() {
                    if (Vars.headless)
                        exit()
                }

                override fun exit() {
                    runBlocking {
                        ScriptManager.disableAll()
                    }
                }
            })
        }

        Log.info("&y===========================")
        Log.info("&lm&fb     ScriptAgent &b$version         ")
        Log.info("&b           By &cWayZer    ")
        Log.info("&b插件官网: https://git.io/SA4Mindustry")
        Log.info("&bQQ交流群: 1033116078")
        if (script == null)
            Log.warn("&c未找到启动脚本(SAMain=$main),请下载安装脚本包,以发挥本插件功能")
        else {
            val all = ScriptRegistry.allScripts { true }
            Log.info(
                "&b共找到${all.size}脚本,加载成功${all.count { it.scriptState.loaded }},启用成功${all.count { it.scriptState.enabled }},出错${all.count { it.failReason != null }}"
            )
        }
        Log.info("&y===========================")
    }

    private fun tryExtract(from: String, to: File) {
        if (to.exists()) return
        to.parentFile.mkdirs()
        val internal = javaClass.classLoader.getResourceAsStream(from) ?: return
        to.writeBytes(internal.readAllBytes())
    }
}