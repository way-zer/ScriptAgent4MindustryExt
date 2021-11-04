package coreLibrary.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.define.ISubScript
import java.util.logging.Logger

val Config.dataDirectory
    get() = rootDir.resolve("data").apply {
        mkdirs()
    }

val ISubScript.dotId get() = id.replace('/', '.')
val ISubScript.logger get() = Logger.getLogger(dotId)!!