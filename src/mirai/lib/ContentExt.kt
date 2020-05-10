package mirai.lib

import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.util.DSLBuilder
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotEvent
import kotlin.reflect.KClass

data class BotListener<E : BotEvent>(val cls: KClass<E>, val listener: suspend E.(E) -> Unit)

val IContentScript.botListeners by DSLBuilder.dataKeyWithDefault { mutableSetOf<BotListener<*>>() }
var IContentScript.bot by DSLBuilder.dataKey<Bot>()

inline fun <reified E : BotEvent> IContentScript.botListen(noinline listener: suspend E.(E) -> Unit) {
    botListeners.add(BotListener(E::class, listener))
}