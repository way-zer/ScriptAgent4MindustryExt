import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.eclipse.jetty.http.HttpStatus
import web.lib.serverList.SharedData

handle {
    get("/servers/list") {
        it.json(SharedData.servers.values)
    }
    get("/servers/add") { ctx ->
        val address = ctx.queryParam("address") ?: let {
            ctx.status(HttpStatus.BAD_REQUEST_400)
            return@get
        }
        ctx.result(SharedCoroutineScope.future(Dispatchers.IO) {
            try {
                SharedData.add(address)
                ctx.status(HttpStatus.OK_200)
            } catch (e: Throwable) {
                ctx.status(HttpStatus.NOT_ACCEPTABLE_406)
                    .result(e.message!!)
            }
        })
    }
}