@file:Depends("coreLibrary")
@file:Import("-Xjvm-default=enable", compileArg = true)
@file:Import("net.mamoe:mirai-core-jvm:2.10.0", mavenDepends = true)
@file:Import("mirai.lib.*", defaultImport = true)
@file:Import("net.mamoe.mirai.event.*", defaultImport = true)
@file:Import("net.mamoe.mirai.event.events.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.data.*", defaultImport = true)
@file:Import("net.mamoe.mirai.contact.*", defaultImport = true)

package mirai

import coreLibrary.lib.event.RequestPermissionEvent
import coreLibrary.lib.util.withContextClassloader
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.MiraiLoggerPlatformBase
import net.mamoe.mirai.utils.StandardCharImageLoginSolver
import java.util.logging.Level

val enable by config.key(false, "是否启动机器人(开启前先设置账号密码)")
val logVerbose by config.key(false, "向控制台输出mirai完整日记")
val qq by config.key(1849301538L, "机器人qq号")
val password by config.key("123456", "机器人qq密码")
val qqProtocol by config.key(
    BotConfiguration.MiraiProtocol.ANDROID_PAD,
    "QQ登录类型，不同的类型可同时登录",
    "可用值: ANDROID_PHONE ANDROID_PAD ANDROID_WATCH"
)

val channel = Channel<String>(onBufferOverflow = BufferOverflow.DROP_LATEST)

inner class MyLoggerImpl(override val identity: String, private val botLog: Boolean) : MiraiLoggerPlatformBase() {
    override fun verbose0(message: String?, e: Throwable?) = debug0(message, e)
    override fun debug0(message: String?, e: Throwable?) {
        if (logVerbose)
            logger.log(Level.INFO, message, e)
    }

    override fun info0(message: String?, e: Throwable?) {
        if (logVerbose || botLog)
            logger.log(Level.INFO, message, e)
    }

    override fun warning0(message: String?, e: Throwable?) {
        logger.log(Level.WARNING, message, e)
    }

    override fun error0(message: String?, e: Throwable?) {
        logger.log(Level.SEVERE, message, e)
    }
}

withContextClassloader { globalEventChannel() }//init

onEnable {
    if (!enable) {
        println("机器人未开启,请先修改配置文件")
        return@onEnable
    }
    val bot = BotFactory.newBot(qq, password) {
        workingDir = Config.dataDir.resolve("mirai").apply { mkdirs() }
        cacheDir = Config.cacheDir.resolve("mirai_cache").relativeTo(workingDir)
        fileBasedDeviceInfo()
        protocol = qqProtocol
        parentCoroutineContext = coroutineContext
        loginSolver = StandardCharImageLoginSolver(channel::receive)
        botLoggerSupplier = { MyLoggerImpl("Bot ${it.id}", true) }
        networkLoggerSupplier = { MyLoggerImpl("Net ${it.id}", false) }
    }
    launch {
        withContextClassloader {
            bot.login()
        }
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

listenTo<RequestPermissionEvent> {
    val subject = subject
    if (subject is User) group += "qq${subject.id}"
}