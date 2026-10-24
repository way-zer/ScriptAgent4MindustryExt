package cf.wayzer.scriptAgent.standalone

import cf.wayzer.libraryManager.MutableURLClassLoader
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.util.CommonMain
import cf.wayzer.scriptAgent.util.MainEventLoop
import kotlinx.coroutines.delay
import java.io.File
import kotlin.system.exitProcess

object Main : CommonMain {
    @JvmStatic
    fun main(args: Array<String>) = MainEventLoop.runBlocking {
        initConfigInfo(
            rootDir = File(System.getenv("SARoot") ?: "scripts"),
            version = javaClass.getResource("/META-INF/ScriptAgent/Version")?.readText() ?: "Unknown Version",
            args = args
        )
        (javaClass.classLoader as MutableURLClassLoader).addURL(File("nativeLibs").toURI().toURL())

        bootstrap()

        if (ScriptRegistry.allScripts { it.enabled }.isEmpty()) {
            println("No Script Enabled")
            exitProcess(-1)
        }
        while (ScriptRegistry.allScripts { it.enabled }.isNotEmpty())
            delay(1_000)
        println("Bye!!")
    }
}