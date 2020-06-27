package cf.wayzer.scriptAgent

import cf.wayzer.libraryManager.Dependency
import cf.wayzer.libraryManager.LibraryManager
import java.io.File
import java.nio.file.Paths

fun main(arg: Array<String?>?) {
    var version = "1.2.7"
    if (arg != null && arg.isNotEmpty() && arg[0] != null) version = arg[0].toString()
    LibraryManager(Paths.get("libs")).apply {
        addRepository("wayzer", "https://dl.bintray.com/way-zer/maven/")
        require(Dependency("cf.wayzer:ScriptAgent:$version"))
    }.getClassloader(LibraryManager::class.java.classLoader).apply {
        val c1 = loadClass("cf.wayzer.script_agent.ScriptAgent")
        val inst1 = c1.getField("INSTANCE").get(null)
        c1.getMethod("load").invoke(inst1)
        val dir = File("scripts")
        println("Load scripts from ${dir.absolutePath}")
        val c2 = loadClass("cf.wayzer.script_agent.ScriptManager")
        val inst2 = c2.getField("INSTANCE").get(null)
        c2.getMethod("loadDir", File::class.java).invoke(inst2, dir)
        println("Finished!")
        while (true) Thread.sleep(60_000)
    }
}