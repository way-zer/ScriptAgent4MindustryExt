import kotlin.concurrent.thread

onEnable {
    thread(true, isDaemon = true) {
        val console = System.console() ?: return@thread
        val consoleSender = Sender()
        while (enabled) {
            val line = console.readLine("> ")
            RootCommands.handle(consoleSender, line)
        }
    }
}