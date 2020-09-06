@file:MavenDepends("org.jline:jline-terminal-jansi:3.15.0")
@file:MavenDepends("org.jline:jline-terminal:3.15.0")
@file:MavenDepends("org.jline:jline-reader:3.15.0")

package coreMindustry

import org.jline.reader.*
import kotlin.concurrent.thread

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        try {
            candidates += RootCommands.tabComplete(null, reader.buffer.toString().split(' ')).map {
                Candidate(it)
            }
        } catch (e: CommandInfo.Return) {
        }
    }
}

onEnable {
    val arr = arrayOfNulls<Thread>(Thread.activeCount())
    Thread.enumerate(arr)
    arr.forEach {
        if (it?.name == "Server Controls")
            it.interrupt()
    }

    thread(true, isDaemon = true, contextClassLoader = javaClass.classLoader, name = "Console Reader") {
        val reader = LineReaderBuilder.builder()
                .completer(MyCompleter).build() as LineReader
        while (enabled) {
            val line = try {
                reader.readLine("> ")
            } catch (e: UserInterruptException) {
                println(e)
                continue
            }
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