package coreMindustry.util

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script
import coreLibrary.lib.util.nextEvent
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.random.Random

data class OnTextInputResult(val player: Player, val id: Int, val text: String?) : Event {
    companion object : Event.Handler()
}

@Suppress("unused")
context(script: Script)
suspend fun textInput(
    player: Player,
    title: String,
    message: String = "",
    default: String = "",
    lengthLimit: Int = Int.MAX_VALUE,
    isNumeric: Boolean = false,
    timeoutMillis: Int = 60_000
): String? = withTimeoutOrNull(timeoutMillis.toLong()) {
    val id = Random.nextInt(Int.MIN_VALUE, 0)
    Call.textInput(player.con, id, title, message, lengthLimit, default, isNumeric)
    script.nextEvent<OnTextInputResult> { it.player == player && it.id == id }.text
}