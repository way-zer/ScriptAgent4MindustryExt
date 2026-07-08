package coreMindustry.util

import mindustry.game.EventType.TextInputEvent

listen<TextInputEvent> {
    OnTextInputResult(it.player, it.textInputId, it.text).launchEmit(coroutineContext + Dispatchers.game)
}

