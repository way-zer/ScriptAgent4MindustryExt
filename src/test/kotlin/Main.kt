import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptManager
import java.io.File

fun main() {
    Config.compilerMode = false
    ScriptManager.loadDir(File("scripts"))
    while (true) Thread.sleep(60_000)
}