@file:DependsModule("coreStandalone")

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import web.lib.handle

val port by config.key(8088, "Web serer port")

addDefaultImport("web.lib.*")
generateHelper()

onEnable {
    Javalin.create().start(port).let { server ->
        JavalinVue.rootDirectory("data/vue", Location.EXTERNAL)
        onAfterContentEnable { item ->
            item.handle.forEach {
                it.invoke(server);true
            }
        }
        onDisable {
            server.stop()
        }
    }
}