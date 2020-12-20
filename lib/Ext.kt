package coreLibrary.lib

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ISubScript
import java.util.logging.Logger

val Config.dataDirectory
    get() = rootDir.resolve("data").apply {
        mkdirs()
    }

val ISubScript.logger get() = Logger.getLogger(id.replace("/", "."))