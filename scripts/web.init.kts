@file:DependsModule("coreStandalone")
@file:MavenDepends("org.slf4j:slf4j-simple:1.7.28")
@file:MavenDepends("javax.servlet:javax.servlet-api:3.1.0")
@file:MavenDepends("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")
@file:MavenDepends("com.fasterxml.jackson.core:jackson-databind:2.10.1")
@file:MavenDepends("com.fasterxml.jackson.core:jackson-core:2.10.1")
@file:MavenDepends("com.fasterxml.jackson.core:jackson-annotations:2.10.1")
@file:MavenDepends("io.javalin:javalin:3.8.0", single = false)

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import web.lib.handle

val port by config.key(8088, "Web serer port")

addDefaultImport("web.lib.*")
addLibraryByName("javalin")
addLibraryByName("kotlinx-coroutines-jdk8")
addLibraryByClass("org.eclipse.jetty.http.HttpStatus")
addLibraryByClass("javax.servlet.http.HttpServletResponse")
generateHelper()

onEnable {
    val bak = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = javaClass.classLoader
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
    Thread.currentThread().contextClassLoader = bak
}