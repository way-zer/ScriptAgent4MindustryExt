package bootStrap

import cf.wayzer.scriptAgent.util.CASScriptPacker
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import java.io.File
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun prepareBuiltin(outputFile: File = File("build/tmp/builtin.packed.zip")) {
    val scripts = ScriptRegistry.allScripts { it.scriptState.loaded }
        .mapNotNull { it.compiledScript }
    println("prepare Builtin for ${scripts.size} scripts.")
    @OptIn(SAExperimentalApi::class)
    CASScriptPacker(outputFile.outputStream())
        .use { scripts.forEach(it::add) }
}

onEnable {
    if (id != Config.mainScript)
        return@onEnable ScriptManager.disableScript(this, "仅可通过SAMAIN启用")
    DependencyManager {
        addRepository("https://www.jitpack.io/")
        require(Dependency.parse("com.github.TinyLake.MindustryX:core:v2025.06.X10"))
        loadToClassLoader(Config.mainClassloader)
    }
    ScriptManager.transaction {
        //compiler plugin
        add("coreLibrary/kcp")
        load();enable()

        if (Config.args.isEmpty())
            addAll()
        else
            Config.args.forEach { add(it) }
        load()
    }
    val fail = ScriptRegistry.allScripts { it.failReason != null }
    println("共加载${ScriptRegistry.allScripts { it.scriptState != ScriptState.Found }.size}个脚本，失败${fail.size}个")
    fail.forEach {
        println("\t${it.id}: ${it.failReason}")
    }
    if (System.getProperty("ScriptAgent.PreparePack") != null) {
        println("Finish pack in ${measureTimeMillis { prepareBuiltin() }}ms")
    }
    exitProcess(fail.size)
}