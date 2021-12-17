@file:Depends("coreLibrary")
@file:Import("net.mamoe:mirai-core-jvm:2.8.1", mavenDepends = true)
@file:Import("mirai.lib.*", defaultImport = true)
@file:Import("net.mamoe.mirai.event.*", defaultImport = true)
@file:Import("net.mamoe.mirai.event.events.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.data.*", defaultImport = true)
@file:Import("net.mamoe.mirai.contact.*", defaultImport = true)

package mirai

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.*
import java.util.logging.Level

val enable by config.key(false, "是否启动机器人(开启前先设置账号密码)")
val qq by config.key(1849301538L, "机器人qq号")
val password by config.key("123456", "机器人qq密码")
val qqProtocol by config.key(
    BotConfiguration.MiraiProtocol.ANDROID_PAD,
    "QQ登录类型，不同的类型可同时登录",
    "可用值: ANDROID_PHONE ANDROID_PAD ANDROID_WATCH"
)

val channel = Channel<String>(onBufferOverflow = BufferOverflow.DROP_LATEST)

onEnable {
    if (!enable) {
        println("机器人未开启,请先修改配置文件")
        return@onEnable
    }
    MiraiLogger.setDefaultLoggerCreator { tag ->
        @OptIn(MiraiInternalApi::class)
        object : PlatformLogger() {
            override fun info0(message: String?, e: Throwable?) {
                if (tag?.startsWith("Bot") == true)
                    logger.log(Level.INFO, message, e)
            }

            override fun info0(message: String?) {
                info0(message, null)
            }

            override fun debug0(message: String?) {}
            override fun debug0(message: String?, e: Throwable?) {}
            override fun verbose0(message: String?) {}
            override fun verbose0(message: String?, e: Throwable?) {}
        }
    }
    val bot = BotFactory.newBot(qq, password) {
        loginCacheEnabled = false
        protocol = qqProtocol
        fileBasedDeviceInfo(Config.dataDirectory.resolve("miraiDeviceInfo.json").absolutePath)
        parentCoroutineContext = coroutineContext
        loginSolver = StandardCharImageLoginSolver(channel::receive)
    }
    launch {
        bot.login()
    }
}

Commands.controlCommand.let {
    it += CommandInfo(this, "mirai", "重定向输入到mirai") {
        usage = "[args...]"
        permission = "mirai.input"
        body {
            channel.trySend(arg.joinToString(" "))
        }
    }
}