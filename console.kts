@file:Import("org.jline:jline-terminal-jansi:3.19.0", mavenDependsSingle = true)
@file:Import("org.jline:jline-terminal:3.19.0", mavenDependsSingle = true)
@file:Import("org.fusesource.jansi:jansi:2.1.0", mavenDependsSingle = true)
@file:Import("org.jline:jline-reader:3.19.0", mavenDependsSingle = true)

package coreMindustry

import org.jline.reader.*
import org.jline.utils.AttributedString
import java.io.ByteArrayOutputStream
import java.io.InterruptedIOException
import java.io.PrintStream
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MyPrintStream(private val block: (String) -> Unit) : PrintStream(ByteArrayOutputStream()) {
    private val bufOut = out as ByteArrayOutputStream

    var last = -1
    override fun write(b: Int) {
        if (last == 13 && b == 10) {// \r\n
            last = -1
            return
        }
        last = b
        if (b == 13 || b == 10) flush()
        else super.write(b)
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        if (len < 0) throw ArrayIndexOutOfBoundsException(len)
        for (i in 0 until len)
            write(buf[off + i].toInt())
    }

    @Synchronized
    override fun flush() {
        val str = try {
            bufOut.toString()
        } finally {
            bufOut.reset()
        }
        block(str)
    }
}

object MyCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        val cmd = line.line().substring(0, line.cursor()).split(' ')
        candidates += RootCommands.tabComplete(null, cmd).map {
            Candidate(it)
        }
    }
}

@OptIn(LoaderApi::class)
fun handleInput(reader: LineReader) {
    var last = 0
    while (!Thread.interrupted()) {
        val line = try {
            reader.readLine("> ").let(RootCommands::trimInput)
        } catch (e: InterruptedIOException) {
            return
        } catch (e: UserInterruptException) {
            if (!enabled) break//script disable
            if (last != 1) {
                reader.printAbove("Interrupt again to force exit application")
                last = 1
                continue
            }
            reader.printAbove("force exit")
            exitProcess(255)
        } catch (e: EndOfFileException) {
            if (last != 2) {
                reader.printAbove("Catch EndOfFile, again to exit application")
                last = 2
                continue
            }
            reader.printAbove("exit")
            runBlocking {
                ScriptManager.disableAll()
            }
            exitProcess(1)
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

lateinit var reader: LineReader
fun start() {
    thread(true, isDaemon = true, contextClassLoader = javaClass.classLoader, name = "Console Reader") {
        reader = LineReaderBuilder.builder()
            .completer(MyCompleter).build() as LineReader

        val bakOut = System.out
        System.setOut(MyPrintStream {
            reader.printAbove(AttributedString.fromAnsi(it))
        })
        try {
            handleInput(reader)
        } finally {
            System.setOut(bakOut)
        }
    }
}

onEnable {
    thread(true, isDaemon = true) {
        val arr = arrayOfNulls<Thread>(Thread.activeCount())
        Thread.enumerate(arr)
        arr.filter { ((it?.name == "Server Controls") || (it?.name == "Console Reader")) }.forEach {
            it!!.interrupt()
            if (it.name == "Server Controls") {
                //Thread "Server Controls" don't have any point to interrupt. Only stop
                @Suppress("DEPRECATION")
                it.stop()
            }
            it.join()
        }
        start()
    }
}