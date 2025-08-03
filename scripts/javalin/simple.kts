package javalin

webRoutes {
    get("/about") { ctx ->
        ctx.result("Powered by Javalin and ScriptAgent")
    }
}