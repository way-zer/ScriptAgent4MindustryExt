package cf.wayzer.scriptAgent

import cf.wayzer.script_agent.ScriptManager
import java.io.File

object Main {
    @JvmStatic
    fun main() {
        ScriptManager.loadDir(File("scripts").apply { mkdir() })
        println("Finished!")
        while (true) Thread.sleep(60_000)
    }
}