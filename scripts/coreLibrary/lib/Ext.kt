package coreLibrary.lib

import cf.wayzer.script_agent.Config

val Config.dataDirectory
    get() = rootDir.resolve("data").apply {
        mkdirs()
    }