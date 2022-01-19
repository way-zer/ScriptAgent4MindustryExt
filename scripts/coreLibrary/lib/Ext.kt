@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package coreLibrary.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.define.Script

@Deprecated("included in ScriptAgent", ReplaceWith("Config.dataDir"))
val Config.dataDirectory
    get() = dataDir

@Deprecated("included in ScriptAgent", level = DeprecationLevel.HIDDEN)
val Script.dotId
    get() = dotId

@Deprecated("included in ScriptAgent", level = DeprecationLevel.HIDDEN)
val Script.logger
    get() = logger