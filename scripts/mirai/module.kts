@file:Depends("coreLibrary")
@file:Import("net.mamoe:mirai-core-jvm:2.15.0", mavenDepends = true)
@file:Import("top.mrxiaom:qsign:1.1.0-beta", mavenDepends = true)
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
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.MiraiLoggerPlatformBase
import net.mamoe.mirai.utils.StandardCharImageLoginSolver
import top.mrxiaom.qsign.QSignService
import java.util.logging.Level
import java.util.logging.Logger

val logVerbose by config.key(false, "向控制台输出mirai完整日记")
val qq by config.key(1849301538L, "机器人qq号")
val password by config.key("", "机器人qq密码")
val qrLogin by config.key(false, "开启扫码登录")
val qqProtocol by config.key(
    BotConfiguration.MiraiProtocol.ANDROID_PAD,
    "QQ登录类型，不同的类型可同时登录",
    "可用值: ANDROID_PHONE ANDROID_PAD ANDROID_WATCH IPAD MACOS"
)

val channel = Channel<String>(onBufferOverflow = BufferOverflow.DROP_LATEST)

inner class LoggerFactory : MiraiLogger.Factory {
    override fun create(requester: Class<*>, identity: String?): MiraiLogger {
        return object : MiraiLoggerPlatformBase() {
            val logger = Logger.getLogger(if (identity != null) "mirai.$identity" else "mirai")
            override val identity: String? get() = identity
            override fun verbose0(message: String?, e: Throwable?) = debug0(message, e)
            override fun debug0(message: String?, e: Throwable?) {
                if (logVerbose)
                    logger.log(Level.INFO, message, e)
            }

            override fun info0(message: String?, e: Throwable?) {
                if (logVerbose || identity == null || identity.startsWith("Bot "))
                    logger.log(Level.INFO, message, e)
            }

            override fun warning0(message: String?, e: Throwable?) {
                logger.log(Level.WARNING, message, e)
            }

            override fun error0(message: String?, e: Throwable?) {
                logger.log(Level.SEVERE, message, e)
            }
        }
    }
}

withContextClassloader {
    net.mamoe.mirai.utils.Services.register(
        MiraiLogger.Factory::class.qualifiedName!!, LoggerFactory::class.qualifiedName!!, ::LoggerFactory
    )
    val txlib = Config.dataDir.resolve("txlib/8.9.73")
    if (txlib.exists()) {
        QSignService.Factory.apply {
            init(Config.dataDir.resolve("txlib/8.9.73"))
            loadProtocols()
            register()
        }
    } else logger.warning("txLib不存在，跳过QSign")
    globalEventChannel()
}//init
Logger.getLogger("com.github.unidbg").level = Level.OFF

onEnable {
    if (password.isEmpty() && !qrLogin) {
        return@onEnable ScriptManager.disableScript(this, "未配置登录方式，请使用`sa config`进行配置")
    }
    val bot = withContextClassloader {
        val auth = if (qrLogin) BotAuthorization.byQRCode() else BotAuthorization.byPassword(password)
        BotFactory.newBot(qq, auth) {
            workingDir = Config.dataDir.resolve("mirai").apply { mkdirs() }
            cacheDir = Config.cacheDir.resolve("mirai_cache").relativeTo(workingDir)
            fileBasedDeviceInfo()
            protocol = qqProtocol
            parentCoroutineContext = coroutineContext
            loginSolver = StandardCharImageLoginSolver({ channel.receive() })
        }
    }
    launch { bot.login() }
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