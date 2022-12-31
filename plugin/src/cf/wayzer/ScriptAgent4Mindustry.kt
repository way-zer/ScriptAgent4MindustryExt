package cf.wayzer

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.ConfigExt.clientCommands
import cf.wayzer.ConfigExt.mainScript
import cf.wayzer.ConfigExt.serverCommands
import cf.wayzer.ConfigExt.version
import cf.wayzer.scriptAgent.*
import cf.wayzer.scriptAgent.define.LoaderApi
import kotlinx.coroutines.runBlocking
import mindustry.Vars
import mindustry.plugin.Plugin
import java.io.File

@OptIn(LoaderApi::class)
class ScriptAgent4Mindustry : Plugin() {
    init {
        if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
            System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n"
            )
        ScriptAgent.load()
    }

    override fun registerClientCommands(handler: CommandHandler) {
        Config.clientCommands = handler
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Config.serverCommands = handler
    }

    override fun init() {
        val defaultMain = "main/bootStrap"
        val version = Vars.mods.getMod(javaClass).meta.version
        val main = System.getenv("SAMain") ?: defaultMain
        Log.info("SAMain=$main")

        Config.version = version
        Config.mainScript = main
        Config.rootDir = Vars.dataDirectory.child("scripts").file()

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