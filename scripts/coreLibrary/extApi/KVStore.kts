@file:Import("com.h2database:h2-mvstore:2.3.232", mavenDependsSingle = true)

package coreLibrary.extApi

import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.h2.mvstore.type.DataType
import org.h2.mvstore.type.StringDataType
import java.util.logging.Level

val store by lazy {
    Config.dataDir.mkdirs()
    MVStore.Builder()
        .fileName(Config.dataDir.resolve("kvStore.mv").path)
        .backgroundExceptionHandler { _, e -> logger.log(Level.SEVERE, "MVStore background error", e) }
        .open()
        .also { onDisable { it.close() } }
}

fun <V> open(name: String, type: DataType<V>) = open(name, type, StringDataType.INSTANCE)
fun <K, V> open(name: String, key: DataType<K>, type: DataType<V>) =
    store.openMap(name, MVMap.Builder<K, V>().apply {
        keyType(key)
        valueType(type)
    })!!