package coreLibrary.lib

import cf.wayzer.script_agent.IContentScript
import org.h2.mvstore.MVStore
import org.h2.mvstore.tx.TransactionStore
import kotlin.reflect.KProperty

object DataStoreApi {
    private lateinit var db: MVStore
    private val transDB by lazy { TransactionStore(db) }

    class DataStoreKey<T : Any>(private val name: String, private val cls: Class<T>, private val default: DataEntity.() -> T) {
        operator fun getValue(t: DataEntity, prop: KProperty<*>): T {
            val inst = t.data[name]
            @Suppress("UNCHECKED_CAST")
            return if (inst != null && cls.isInstance(inst)) inst as T
            else default(t)
        }

        operator fun setValue(t: RWDataEntity, prop: KProperty<*>, v: T?) {
            if (v == null) t.data.remove(name)
            else t.data[name] = v
        }
    }

    data class DBKey(val name: String) {
        operator fun get(key: String): DataEntity {
            return DataEntity("$name@$key", db.openMap("$name@$key"))
        }

        operator fun contains(key: String): Boolean = db.hasMap("$name@$key")
    }

    open class DataEntity(val key: String, val data: MutableMap<String, Any>) {
        open fun transaction(body: RWDataEntity.() -> Unit) {
            transDB.begin().apply {
                body(RWDataEntity(key, openMap(key)))
                if (hasChanges()) commit()
            }
        }
    }

    class RWDataEntity(key: String, data: MutableMap<String, Any>) : DataEntity(key, data) {
        override fun transaction(body: RWDataEntity.() -> Unit) = error("Already in transaction")
    }

    fun open(filePath: String) {
        db = MVStore.Builder().fileName(filePath).compress().open()!!
    }

    fun close() = db.close()
}

inline fun <reified T : Any> dataStoreKey(name: String, noinline default: DataStoreApi.DataEntity.() -> T) = DataStoreApi.DataStoreKey(name, T::class.java, default)
fun IContentScript.dbKey(name: String) = DataStoreApi.DBKey(name)
val playerData = DataStoreApi.DBKey("player")
