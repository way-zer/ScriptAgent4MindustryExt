@file:Import("org.litote.kmongo:kmongo-coroutine:4.8.0", mavenDepends = true)
@file:Import("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1", mavenDepends = true)
@file:Import("./lib/Mongo.kt", sourceFile = true)

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.util.KMongoConfiguration
import java.util.logging.Level

val addr by config.key("mongodb://localhost", "mongo地址", "重载生效")
onEnable {
    try {
        withContextClassloader {
            val client = KMongo.createClient().coroutine
            onDisable { withContext(Dispatchers.IO) { client.close() } }
            Services.provide<CoroutineClient>(KMongo.createClient(addr).coroutine)
            KMongoConfiguration.registerBsonModule(JavaTimeModule())
        }
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "连接Mongo数据库失败: $addr", e)
        return@onEnable ScriptManager.disableScript(this, "连接Mongo数据库失败: $e")
    }
}