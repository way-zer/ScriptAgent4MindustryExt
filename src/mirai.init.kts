@file:DependsModule("coreLibrary")
@file:MavenDepends("net.mamoe:mirai-core:1.1.3", single = false)
@file:MavenDepends("net.mamoe:mirai-core-qqandroid:1.1.3", single = false)

import arc.util.Log
import cf.wayzer.script_agent.Config
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.DefaultLogger
import net.mamoe.mirai.utils.SimpleLogger

addDefaultImport("mirai.lib.*")
addLibraryByClass("net.mamoe.mirai.Bot")
addDefaultImport("net.mamoe.mirai.Bot")
addDefaultImport("net.mamoe.mirai.event.*")
addDefaultImport("net.mamoe.mirai.message.*")
addDefaultImport("net.mamoe.mirai.message.data.*")
generateHelper()

val enable by config.key(false, "是否启动机器人(开启前先设置账号密码)")
val qq by config.key(1849301538L, "机器人qq号")
val password by config.key("123456", "机器人qq密码")

onEnable {
    if (!enable) {
        println("机器人未开启,请先修改配置文件")
        return@onEnable
    }
    DefaultLogger = {
        SimpleLogger { priority, msg, throwable ->
            when (priority) {
                SimpleLogger.LogPriority.WARNING -> {
                    Log.warn("[$it]$msg", throwable)
                }
                SimpleLogger.LogPriority.ERROR -> {
                    Log.err("[$it]$msg", throwable)
                }
                SimpleLogger.LogPriority.INFO -> {
                    if (it?.startsWith("Bot") == true)
                        Log.info("[$it]$msg", throwable)
                }
                else -> {
                    // ignore
                }
            }
        }
    }
    val bot = Bot(qq, password) {
        fileBasedDeviceInfo(Config.dataDirectory.resolve("miraiDeviceInfo.json").absolutePath)
        parentCoroutineContext = coroutineContext
    }
    launch {
        bot.login()
    }
}