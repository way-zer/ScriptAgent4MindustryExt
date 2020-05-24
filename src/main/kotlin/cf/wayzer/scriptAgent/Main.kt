package cf.wayzer.scriptAgent

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptManager
import java.io.File

fun main() {
    val dir = File("scripts")
    Config.compilerMode = false
    println("Load scripts from ${dir.absolutePath}")
    ScriptManager.loadDir(dir)
    println("Finished!")
    while (true) Thread.sleep(60_000)
}