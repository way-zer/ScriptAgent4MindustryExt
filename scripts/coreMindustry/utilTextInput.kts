package coreMindustry

import coreLibrary.lib.util.nextEvent
import mindustry.game.EventType.TextInputEvent
import kotlin.random.Random

data class OnTextInputResult(val player: Player, val id: Int, val text: String?) : Event {
    companion object : Event.Handler()
}

listen<TextInputEvent> {
    OnTextInputResult(it.player, it.textInputId, it.text).launchEmit()
}

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
    nextEvent<OnTextInputResult> { it.player == player && it.id == id }.text
}
export(::textInput)