@file:DependsModule("coreMindustry")
@file:MavenDepends("net.mamoe:mirai-core:0.39.5", single = false)
@file:MavenDepends("net.mamoe:mirai-core-qqandroid:0.39.5", single = false)

import kotlinx.coroutines.launch
import mirai.lib.BotListener
import mirai.lib.bot
import mirai.lib.botListeners
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribe
import net.mamoe.mirai.join

addDefaultImport("mirai.lib.*")
addLibraryByClass("net.mamoe.mirai.Bot")
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
    val bot = Bot(qq, password)
    SharedCoroutineScope.launch {
        bot.alsoLogin()
        onBeforeContentEnable { it.bot = bot }
        onAfterContentEnable { item ->
            item.botListeners.forEach {
                fun <E : BotEvent> BotListener<E>.listen() {
                    bot.subscribe(cls) {
                        listener(this)
                        if (item.enabled) ListeningStatus.LISTENING else ListeningStatus.STOPPED
                    }
                }
                it.listen()
            }
        }
        bot.join()
    }
    onDisable {
        bot.close()
    }
}