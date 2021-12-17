package coreLibrary.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.define.Script
import java.util.logging.Logger

val Config.dataDirectory
    get() = rootDir.resolve("data").apply {
        mkdirs()
    }

val Script.dotId get() = id.replace('/', '.')
val Script.logger get() = Logger.getLogger(dotId)!!