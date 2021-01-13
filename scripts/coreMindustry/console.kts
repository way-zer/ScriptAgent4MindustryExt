@file:MavenDepends("org.jline:jline-terminal-jansi:3.15.0")
@file:MavenDepends("org.jline:jline-terminal:3.15.0")
@file:MavenDepends("org.jline:jline-reader:3.15.0")

package coreMindustry

import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.ScriptManager
import org.jline.reader.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        candidates += RootCommands.tabComplete(null, reader.buffer.toString().split(' ')).map {
            Candidate(it)
        }
    }
}

onEnable {
    val arr = arrayOfNulls<Thread>(Thread.activeCount())
    Thread.enumerate(arr)
    arr.forEach {
        if (it?.name == "Server Controls" || it?.name == "Console Reader")
            it.interrupt()
    }

    thread(true, isDaemon = true, contextClassLoader = javaClass.classLoader, name = "Console Reader") {
        val reader = LineReaderBuilder.builder()
            .completer(MyCompleter).build() as LineReader
        var last = 0
        while (!Thread.interrupted()) {
            val line = try {
                reader.readLine("> ").trim()
            } catch (e: UserInterruptException) {
                if (last != 1) {
                    println("Interrupt again to exit application")
                    last = 1
                    continue
                }
                println("exit")
                ScriptManager.disableAll()
                exitProcess(255)
            } catch (e: EndOfFileException) {
                if (last != 2) {
                    println("Catch EndOfFile, again to reload all script")
                    last = 2
                    continue
                }
                println("Catch EndOfFile, reload all script")
                ScriptManager.disableAll()
                ScriptManager.loadedInitScripts.clear()
                ScriptManager.loadDir(Config.rootDir)
                return@thread
            }
            last = 0
            if (line.isEmpty()) continue
            try {
                RootCommands.invoke(CommandContext().apply {
                    hasPermission = { true }
                    arg = line.split(' ')
                    reply = {
                        ContentHelper.logToConsole(it.toString())
                    }
                })
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}