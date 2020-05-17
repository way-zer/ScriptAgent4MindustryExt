package web.serverList

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.eclipse.jetty.http.HttpStatus
import web.lib.serverList.PingUtil.Info
import web.lib.serverList.SharedData

handle {
    get("/servers/list") { context ->
        context.json(SharedData.servers.values.sortedWith(Comparator<Info> { o1, o2 ->
            if (o1.online && !o2.online) -1 else 0
        }.thenByDescending { it.players }))
    }
    get("/servers/add") { ctx ->
        val address = ctx.queryParam("address") ?: let {
            ctx.status(HttpStatus.BAD_REQUEST_400)
            return@get
        }
        ctx.result(SharedCoroutineScope.future(Dispatchers.IO) {
            try {
                val result = SharedData.add(address)
                if (result == "OK")
                    ctx.status(HttpStatus.OK_200)
                else
                    ctx.status(HttpStatus.FORBIDDEN_403).result(result)
            } catch (e: Throwable) {
                ctx.status(HttpStatus.NOT_ACCEPTABLE_406)
                    .result(e.message!!)
            }
        })
    }
}