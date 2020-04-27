handle {
    get("/servers") {
        if (it.path() != "/servers/")
            it.redirect("/servers/")
        else
            html(it, "index.html")
    }
    get("/servers/main.js") {
        it.result(resDir.resolve("main.js").inputStream())
    }
}