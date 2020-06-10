@file:Suppress("unused")

package coreLibrary.lib

/**
 * 持久储存Api
 * 可将该Api作为数据库使用
 * @sample
 * var DataStoreApi.DataEntity.testKey by dataStoreKey("money"){0}
 * playerData["fakeUUID"].run {
 * println(testKey)
 * testKey+=50
 * println(testKey)
 * }
 */
import cf.wayzer.script_agent.IBaseScript
import org.h2.mvstore.MVStore
import kotlin.reflect.KProperty

object DataStoreApi {
    private lateinit var db: MVStore

    class DataStoreKey<T : Any>(
        private val name: String,
        private val cls: Class<T>,
        private val default: DataEntity.() -> T
    ) {
        operator fun getValue(t: DataEntity, prop: KProperty<*>): T {
            val inst = t.data[name]
            return if (cls.isInstance(inst)) cls.cast(inst)
            else default(t)
        }

        operator fun setValue(t: DataEntity, prop: KProperty<*>, v: T?) {
            if (v == null) t.data.remove(name)
            else t.data[name] = v
        }
    }

    data class DBKey(val name: String) {
        operator fun get(key: String): DataEntity {
            return DataEntity("$name@$key", db.openMap("$name@$key"))
        }

        fun <K, V> subMap(key: String) = DataCollections("$name#$key", db.openMap<K, V>("$name#$key"))
        operator fun contains(key: String): Boolean = db.hasMap("$name@$key")
    }

    open class DataEntity(val key: String, val data: MutableMap<String, Any>)
    open class DataCollections<K, V>(val key: String, map: MutableMap<K, V>) : MutableMap<K, V> by map

    fun open(filePath: String) {
        db = MVStore.Builder().fileName(filePath).compress().open()!!
    }

    fun close() = db.close()
}

inline fun <reified T : Any> dataStoreKey(name: String, noinline default: DataStoreApi.DataEntity.() -> T) =
    DataStoreApi.DataStoreKey(name, T::class.java, default)

fun IBaseScript.dbKey(name: String) = DataStoreApi.DBKey(name)
val playerData = DataStoreApi.DBKey("player")
