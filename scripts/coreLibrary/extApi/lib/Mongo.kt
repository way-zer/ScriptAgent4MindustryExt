package coreLib.extApi

import cf.wayzer.scriptAgent.Services
import coreLibrary.lib.get
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection

// @file:Depends("coreLibrary/extApi/mongoApi")
object Mongo {
    val service = Services.get<CoroutineClient>()
    fun get() = service.get()

    const val defaultDBName = "DEFAULT"
    fun getDB(db: String = defaultDBName) = get().getDatabase(db)
    inline fun <reified T : Any> collection(db: String = defaultDBName): CoroutineCollection<T> {
        return getDB(db).getCollection()
    }
}