@file:MavenDepends("org.jline:jline-terminal-jansi:3.15.0")
@file:MavenDepends("org.jline:jline-terminal:3.15.0")
@file:MavenDepends("org.jline:jline-reader:3.15.0")

package coreStandalone

import org.jline.reader.*
import kotlin.concurrent.thread

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        try {
            RootCommands.onComplete(CommandContext().apply {
                hasPermission = { true }
                arg = reader.buffer.toString().split(' ')
                replyTabComplete = {
                    it.forEach { s ->
                        candidates.add(Candidate(s))
                    }
                    CommandInfo.Return()
                }
            })
        } catch (e: CommandInfo.Return) {
        }
    }
}

onEnable {
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
                        println(ColorApi.handle(it.toString(), ColorApi::consoleColorHandler))
                    }
                })
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}