import kotlin.concurrent.thread

onEnable {
    thread(true, isDaemon = true) {
        val consoleSender = Sender()
        val console = System.console() ?: let {
            println("No console,use System.in")
            return@thread System.`in`.reader().useLines { lines ->
                lines.forEach {
                    RootCommands.handle(consoleSender, it)
                }
            }
        }
        while (enabled) {
            val line = console.readLine("> ")
            RootCommands.handle(consoleSender, line)
        }
    }
}