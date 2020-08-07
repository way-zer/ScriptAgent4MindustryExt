@file:MavenDepends("org.jline:jline-terminal-jansi:3.15.0")
@file:MavenDepends("org.jline:jline-terminal:3.15.0")
@file:MavenDepends("org.jline:jline-reader:3.15.0")

import org.jline.reader.*
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter
import kotlin.concurrent.thread

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        println(line.line())
//        RootCommands.invoke(CommandContext().apply {
//            hasPermission = {true}
//            thisCommand = CommandInfo(null,"",""){}
//            arg =
//            replyTabComplete = {
//                it.forEach { s->
//                    candidates.add(Candidate(s))
//                }
//                CommandInfo.Return()
//            }
//        })
    }
}

class LogOutStream(val logger: Logger, val level: Level) : ByteArrayOutputStream() {
    override fun flush() {
        logger.log(level, toString(Charsets.UTF_8.name()))
        reset()
    }
}

onEnable {
    thread(true, isDaemon = true, contextClassLoader = javaClass.classLoader, name = "Console Reader") {
        val reader = LineReaderBuilder.builder()
            .completer(MyCompleter).build() as LineReader
        Logger.getGlobal().apply {
            handlers.forEach(this::removeHandler)
            addHandler(object : Handler() {
                init {
                    formatter = object : Formatter() {
                        val dataFormat = SimpleDateFormat("MMdd-hh:mm:ss")
                        override fun format(record: LogRecord): String {
                            val thrower = record.thrown?.let { e ->
                                val sw = StringWriter()
                                PrintWriter(sw).use {
                                    it.println()
                                    e.printStackTrace(it)
                                }
                                sw.toString()
                            } ?: ""
                            return "${dataFormat.format(Date(record.millis))}[${record.level}][${record.loggerName}]" +
                                    "${formatMessage(record)}$thrower"
                        }
                    }
                }

                @Volatile
                var toWrite = ""
                override fun publish(record: LogRecord) {
                    if (!isLoggable(record)) return
                    try {
                        toWrite = formatter.format(record)
                        flush()
                    } catch (e: Exception) {
                        reportError(null, e, ErrorManager.FORMAT_FAILURE)
                    }
                }

                override fun flush() {
                    if (!toWrite.isBlank())
                        reader.printAbove(toWrite)
                }

                override fun close() {
                    flush()
                }
            })
//            System.setOut(PrintStream(LogOutStream(this, Level.INFO), true))
//            System.setErr(PrintStream(LogOutStream(this, Level.SEVERE), true))
        }
        while (enabled) {
            val line = reader.readLine("> ")
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