package javalin

routing {
    get("/about") { ctx ->
        ctx.result("Powered by Javalin and ScriptAgent")
    }
}