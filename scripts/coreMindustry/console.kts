@file:Import("org.jline:jline-terminal-jansi:3.19.0", mavenDependsSingle = true)
@file:Import("org.jline:jline-terminal:3.19.0", mavenDependsSingle = true)
@file:Import("org.fusesource.jansi:jansi:2.1.0", mavenDependsSingle = true)
@file:Import("org.jline:jline-reader:3.19.0", mavenDependsSingle = true)

package coreMindustry

import org.jline.reader.*
import java.io.InterruptedIOException
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        candidates += RootCommands.tabComplete(null, reader.buffer.toString().split(' ')).map {
            Candidate(it)
        }
    }
}

lateinit var reader: LineReader

onEnable {
    lateinit var thisT: Thread
    thisT = thread(true, isDaemon = true, contextClassLoader = javaClass.classLoader, name = "Console Reader") {
        val arr = arrayOfNulls<Thread>(Thread.activeCount())
        Thread.enumerate(arr)
        arr.filter { ((it?.name == "Server Controls") || (it?.name == "Console Reader")) && (it != thisT) }.forEach {
            it!!.interrupt()
            if (it.name == "Server Controls") {
                //Thread "Server Controls" don't have any point to interrupt. Only stop
                @Suppress("DEPRECATION")
                it.stop()
            }
            it.join()
        }
        reader = LineReaderBuilder.builder()
            .completer(MyCompleter).build() as LineReader
        var last = 0
        while (!Thread.interrupted()) {
            val line = try {
                reader.readLine("> ").let(RootCommands::trimInput)
            } catch (e: InterruptedIOException) {
                return@thread
            } catch (e: UserInterruptException) {
                if (last != 1) {
                    reader.printAbove("Interrupt again to exit application")
                    last = 1
                    continue
                }
                reader.printAbove("exit")
                ScriptManager.disableAll()
                exitProcess(255)
            } catch (e: EndOfFileException) {
                if (last != 2) {
                    reader.printAbove("Catch EndOfFile, again to reload all script")
                    last = 2
                    continue
                }
                reader.printAbove("Catch EndOfFile, reload all script")
                ScriptManager.disableAll()
                @OptIn(LoaderApi::class)
                ScriptManager.loadDir(Config.rootDir, true)
                return@thread
            }
            last = 0
            if (line.isEmpty()) continue
            try {
                RootCommands.handleInput(line, null)
            } catch (e: Throwable) {
                logger.log(Level.SEVERE, "error when handle input", e)
            }
        }
    }
}