package coreLibrary.lib.event

import cf.wayzer.scriptAgent.Event
import coreLibrary.lib.PermissionApi

@Suppress("MemberVisibilityCanBePrivate")
class RequestPermissionEvent(val subject: Any, val permission: String, var group: List<String> = emptyList()) :
    Event, Event.Cancellable {
    var directReturn: PermissionApi.Result? = null
    fun directReturn(result: PermissionApi.Result) {
        directReturn = result
    }

    override val handler = Companion

    companion object : Event.Handler()

    override var cancelled: Boolean
        get() = directReturn != null
        set(value) {
            if (value) directReturn(PermissionApi.Result.Reject)
        }
}