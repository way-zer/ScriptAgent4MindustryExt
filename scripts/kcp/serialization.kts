@file:Import("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0", mavenDepends = true)

package kcp

import cf.wayzer.scriptAgent.events.ScriptCompileEvent
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import java.io.File

var pluginFile: File? = null

suspend fun getOrLoad(): File {
    if (pluginFile == null) {
        pluginFile = DependencyManager {
            val dep = "org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:${Config.kotlinVersion}"
            require(Dependency.parse(dep), resolveChild = false)
            load()
            getFiles().single()
        }
    }
    return pluginFile!!
}

listenTo<ScriptCompileEvent> {
    if (!dependsOn(scriptInfo)) return@listenTo
    addCompileOptions("-Xplugin=${getOrLoad().absolutePath}")
}