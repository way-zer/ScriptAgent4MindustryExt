@file:Import("tools.profiler:async-profiler:4.1", mavenDepends = true)
@file:Import("https://repo1.maven.org/maven2/", mavenRepository = true)

package wayzer.ext

import one.profiler.AsyncProfiler
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

val command by config.key("start,jfr,event=cpu,interval=500us,file=FILE", "采样器启动命令, FILE会被替换为实际文件路径")
var running: DisposableHandle? = null

fun start(cmd: String) {
    val profiler = AsyncProfiler.getInstance()
    val file = Config.cacheDir.resolve("${Instant.now()}.jfr")
    val start = Instant.now()
    profiler.execute(cmd.replace("FILE", file.absolutePath))
    logger.info("Profiler started, output file: ${file.absolutePath}")
    running = DisposableHandle {
        val elapsed = Duration.between(start, Instant.now()).toKotlinDuration()
        profiler.execute("stop")
        logger.info("Profiler stopped, elapsed time: $elapsed")
        logger.info("Output file: ${file.absolutePath}")

    }
}

onDisable {
    running?.dispose()
    running = null
}

command("profiler", "性能采样") {
    requirePermission(dotId)
    usage = "[command]"
    body {
        running?.let {
            running = null
            it.dispose()
            return@body
        }
        start(arg.firstOrNull() ?: command)
    }
}

