package web

handle {
    get("/") { ctx ->
        ctx.redirect("/servers/")
    }
}