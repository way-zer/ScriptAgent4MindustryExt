@file:Import("org.litote.kmongo:kmongo-coroutine:4.8.0", mavenDepends = true)
@file:Import("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1", mavenDepends = true)


package coreLibrary.extApi

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import coreLibrary.lib.util.ServiceRegistry
import coreLibrary.lib.util.withContextClassloader
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.util.KMongoConfiguration
import java.util.logging.Level

@Suppress("unused")//Api
object Mongo : ServiceRegistry<CoroutineClient>() {
    const val defaultDBName = "DEFAULT"
    fun getDB(db: String = defaultDBName) = get().getDatabase(db)
    inline fun <reified T : Any> collection(db: String = defaultDBName): CoroutineCollection<T> {
        return getDB(db).getCollection()
    }
}

val addr by config.key("mongodb://localhost", "mongo地址", "重载生效")
onEnable {
    try {
        withContextClassloader {
            Mongo.provide(this, KMongo.createClient(addr).coroutine)
            KMongoConfiguration.registerBsonModule(JavaTimeModule())
        }
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "连接Mongo数据库失败: $addr", e)
        return@onEnable ScriptManager.disableScript(this, "连接Mongo数据库失败: $e")
    }
}

onDisable {
    Mongo.getOrNull()?.let {
        withContext(Dispatchers.IO) { it.close() }
    }
}