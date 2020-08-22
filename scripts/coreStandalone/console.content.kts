@file:ImportByClass("org.jline.reader.Completer")//Embedded in kotlin compiler
//@file:MavenDepends("org.jline:jline-terminal-jansi:3.15.0")
//@file:MavenDepends("org.jline:jline-terminal:3.15.0")
//@file:MavenDepends("org.jline:jline-reader:3.15.0")

import org.jline.reader.*
import kotlin.concurrent.thread

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        try {
            RootCommands.invoke(CommandContext().apply {
                hasPermission = { true }
                thisCommand = CommandInfo(null, "", "") {}
                arg = reader.buffer.toString().split(' ')
                replyTabComplete = {
                    it.forEach { s ->
                        candidates.add(Candidate(s, s, null, null, null, null, true))
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
            launch {
                try {
                    RootCommands.invoke(CommandContext().apply {
                        hasPermission = { true }
                        thisCommand = CommandInfo(null, "", "") {}
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
}