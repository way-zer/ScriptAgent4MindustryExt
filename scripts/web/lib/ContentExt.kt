package web.lib

import cf.wayzer.script_agent.IBaseScript
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.util.DSLBuilder
import io.javalin.Javalin
import io.javalin.http.Context

typealias JavalinHandler = Javalin.() -> Unit

val IContentScript.handle by DSLBuilder.callbackKey<JavalinHandler>()

fun IBaseScript.html(ctx: Context, name: String) {
    ctx.html(resDir.resolve(name).readText())
}

val IBaseScript.resDir get() = sourceFile.parentFile.resolve("res")