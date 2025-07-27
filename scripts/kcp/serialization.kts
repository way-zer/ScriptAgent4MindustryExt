package coreLibrary.kcp

import cf.wayzer.scriptAgent.define.annotations.ImportData
import cf.wayzer.scriptAgent.events.ScriptCompileEvent
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency

val library = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0"
val pluginFile by lazy {
    DependencyManager {
        val dep = "org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:${Config.kotlinVersion}"
        require(Dependency.parse(dep), resolveChild = false)
        load()
        getFiles().single()
    }
}

@OptIn(SAExperimentalApi::class)
listenTo<ScriptCompileEvent> {
    if (!script.scriptInfo.dependsOn(thisScript.scriptInfo)) return@listenTo
    registerImportData(ImportData(ImportData.Type.MavenDepends, library))
    addCompileOptions("-Xplugin=${pluginFile.absolutePath}")
}