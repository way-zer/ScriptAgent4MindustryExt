import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptManager
import java.io.File

fun main() {
    Config.compilerMode = false
    ScriptManager.loadDir(File("scripts"))
    while (true) Thread.sleep(60_000)
}