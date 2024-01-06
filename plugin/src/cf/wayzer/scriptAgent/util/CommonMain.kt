package cf.wayzer.scriptAgent.util

import cf.wayzer.scriptAgent.*
import kotlinx.coroutines.runBlocking

interface CommonMain {
    private suspend fun doStart(): Boolean {
        val mainScript = ScriptRegistry.findScriptInfo(Config.mainScript) ?: return false
        ScriptManager.transaction {
            add(mainScript)
            load();enable()
        }
        return true
    }

    fun bootstrap() {
        MainScriptsHelper.load()
        ScriptRegistry.registries.add(BuiltinScriptRegistry)
        ScriptRegistry.scanRoot()
        val foundMain = runBlocking { doStart() }
        displayInfo(foundMain)
    }

    fun displayInfo(foundMain: Boolean) {
        Config.logger.info("===========================")
        Config.logger.info("     ScriptAgent ${Config.version}         ")
        Config.logger.info("           By WayZer    ")
        Config.logger.info("QQ交流群: 1033116078")
        if (foundMain) {
            val all = ScriptRegistry.allScripts { true }
            Config.logger.info(
                "共找到${all.size}脚本,加载成功${all.count { it.scriptState.loaded }},启用成功${all.count { it.scriptState.enabled }},出错${all.count { it.failReason != null }}"
            )
        } else
            Config.logger.warning("未找到启动脚本(SAMain=${Config.mainScript}),请下载安装脚本包,以发挥本插件功能")
        Config.logger.info("===========================")
    }
}