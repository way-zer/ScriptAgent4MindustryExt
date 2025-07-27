@file:Import("tools.profiler:async-profiler:4.1", mavenDepends = true)
@file:Import("https://repo1.maven.org/maven2/", mavenRepository = true)

package wayzer.ext

import one.profiler.AsyncProfiler
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

var running: DisposableHandle? = null

fun start() {
    val profiler = AsyncProfiler.getInstance()
    val file = Config.cacheDir.resolve("${Instant.now()}.jfr")
    val start = Instant.now()
    profiler.execute("start,jfr,event=cpu,file=${file.absolutePath}")
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
    permission = dotId
    body {
        running?.let {
            running = null
            it.dispose()
            return@body
        }
        start()
    }
}

