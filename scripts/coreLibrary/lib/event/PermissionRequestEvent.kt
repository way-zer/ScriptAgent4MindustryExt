package coreLibrary.lib.event

import cf.wayzer.script_agent.Event
import coreLibrary.lib.CommandContext

class PermissionRequestEvent(val permission: String, val context: CommandContext) : Event, Event.Cancellable {
    var result: Boolean? = null
    override var cancelled: Boolean
        get() = result != null
        set(@Suppress("UNUSED_PARAMETER") value) {
            error("Can't cancel,please set result")
        }
    override val handler = Companion

    companion object : Event.Handler()
}