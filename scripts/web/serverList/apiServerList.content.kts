import web.lib.serverList.SharedData

handle {
    get("/servers.json") { context ->
        context.json(SharedData.servers.filter { it.value.online }.map { mapOf("address" to it.key) })
    }
}