@file:Import("io.javalin:javalin:6.7.0", mavenDepends = true)
@file:Import("org.slf4j:slf4j-simple:2.0.16", mavenDepends = true)
@file:Import("javalin.lib.*", defaultImport = true)
@file:Depends("coreLibrary")

package javalin

import cf.wayzer.scriptAgent.events.ScriptDisableEvent
import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import io.javalin.Javalin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.StatisticsHandler
import java.util.logging.Level

val port by config.key(7001, "Javalin 端口")

fun startJavalin(server: Server): Javalin {
    val app = Javalin.create { config ->
        config.pvt.jetty.server = server
        config.startupWatcherEnabled = false
        ScriptRegistry.allScripts { it.enabled }.forEach {
            it.inst?.configJavalin?.forEach {
                it(config)
            }
        }
    }
    ScriptRegistry.allScripts { it.enabled }.forEach {
        it.inst?.webRoutes?.forEach {
            it(app)
        }
    }
    withContextClassloader { app.start() }
    return app
}


onEnable {
    val server = Server(port).apply {
        insertHandler(StatisticsHandler())
    }
    val initHandler = server.handler ?: null
    onDisable { server.stop() }

    restartFlow.emit(Unit)

    @OptIn(FlowPreview::class)
    restartFlow.debounce(1000)
        .onEach {
            logger.info("(Re)Starting Javalin server...")
            try {
                //reset handler
                server.stop()
                server.handler = initHandler
                startJavalin(server)
            } catch (e: Throwable) {
                logger.log(Level.WARNING, "Exception when start", e)
            }
        }.launchIn(this)
}

val restartFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
listenTo<ScriptEnableEvent>(Event.Priority.Watch) {
    if (script.dslExists(configJavalin) || script.dslExists(webRoutes)) {
        restartFlow.emit(Unit)
    }
}

listenTo<ScriptDisableEvent>(Event.Priority.Watch) {
    if (script.dslExists(configJavalin) || script.dslExists(webRoutes)) {
        restartFlow.emit(Unit)
    }
}
