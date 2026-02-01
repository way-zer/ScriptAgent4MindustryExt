package bootStrap

import cf.wayzer.scriptAgent.state.ConditionState
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
    CASScriptPacker(outputFile.outputStream())
        .use { scripts.forEach(it::add) }
}

onEnable {
    if (id != Config.mainScript)
        return@onEnable ScriptManager.disableScript(this, "仅可通过SAMAIN启用")
    ScriptManager.afterTransaction { main() }
}
suspend fun main() {
    DependencyManager {
        addRepository("https://www.jitpack.io/")
        require(Dependency.parse("com.github.TinyLake.MindustryX:core:v2025.10.X21"))
        loadToClassLoader(Config.mainClassloader)
    }
    ScriptManager.transactionV2 {
        //Load Kcp
        enable("kcp")
        execute().printResult()

        if (Config.args.isEmpty())
            compileOnly("")
        else
            Config.args.forEach { compileOnly(it) }
    }.run {
        val fail = conditions.values.filter { it.status == ConditionState.Status.Success }
        println("共加载${conditions.size}个脚本，失败${fail.size}个")
        printResult()
        if (System.getProperty("ScriptAgent.PreparePack") != null) {
            println("Finish pack in ${measureTimeMillis { prepareBuiltin() }}ms")
        }
        exitProcess(fail.size)
    }
}