package coreLibrary.lib

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IContentScript
import java.util.logging.Logger

val Config.dataDirectory
    get() = rootDir.resolve("data").apply {
        mkdirs()
    }

val IContentScript.logger get() = Logger.getLogger(id.replace("/", "."))