package wayzer.lib.dao

/**
 * @author WayZer
 * 自制数据库Entity类,支持在非transaction修改entity属性,最终通过save保存(不依赖exposed-dao.jar)
 * 未来将移动到coreLib中
 */
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import kotlin.reflect.KProperty

open class CacheEntity<ID : Comparable<ID>>(val table: IdTable<ID>) {
    @RequiresOptIn("need in Transaction{}",level = RequiresOptIn.Level.WARNING)
    @OptIn(NeedTransaction::class)
    annotation class NeedTransaction
    private val changed = mutableMapOf<Column<*>, Any?>()
    private lateinit var resultRow: ResultRow
    var new = true
        private set
    var id: EntityID<ID> by table.id
        private set

    /**
     * @return false can't find
     */
    @NeedTransaction
    fun load(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean {
        val resultRow = table.select(where).firstOrNull()
        return resultRow != null && load(resultRow)
    }

    open fun load(resultRow: ResultRow): Boolean {
        this.resultRow = resultRow
        changed.clear()
        new = false
        return true
    }

    /**
     * @return false can't find
     */
    @NeedTransaction
    fun loadById(id: ID): Boolean = load {
        table.id eq EntityID(id, table)
    }

    @NeedTransaction
    fun reload() = loadById(id.value)

    @NeedTransaction
    open fun save(id: ID? = null) {
        fun <T> UpdateBuilder<*>.set(k: Column<T>, v: Any?) {
            @Suppress("UNCHECKED_CAST")
            set(k, v as T)
        }
        if (new) {
            loadById(table.insertAndGetId {
                changed.forEach { (k, v) -> it.set(k, v) }
            }.value)
        } else if (changed.isNotEmpty()) {
            if (id != null) this.id = EntityID(id, table)
            table.update({ table.id eq id }) {
                changed.forEach { (k, v) -> it.set(k, v) }
            }
            reload()
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> Column<T>.getValue(obj: CacheEntity<*>, desc: KProperty<*>): T {
        if (this in changed) return changed[this] as T
        if (new)
            return defaultValueFun?.invoke() ?: error("not init,load first")
        return resultRow[this]
    }

    operator fun <T> Column<T>.setValue(obj: CacheEntity<*>, desc: KProperty<*>, v: T) {
        if (new || this.getValue(obj, desc) != v)
            changed[this] = v
    }
    open class EntityClass<ID : Comparable<ID>,T:CacheEntity<ID>>(val factory:()->T){
        private val cache = mutableMapOf<ID,T>()
        protected val allCached get() = cache.values as Collection<T>
        protected fun addCache(t:T):T{
            if(t.id.value in cache) error("$t already in cache")
            cache[t.id.value] = t
            return t
        }
        open fun removeCache(id:ID):T? = cache.remove(id)?.apply {
            if(changed.isNotEmpty()) error("Entity CHANGE don't save $this")
        }
        fun getOrNull(id:ID) = cache[id]
        @NeedTransaction
        fun getOrFind(id:ID) = cache.getOrPut(id){
            factory().apply {
                if(!loadById(id))error("Can't find")
            }
        }
        @NeedTransaction
        fun findOrCreate(id:ID,default: T.() -> Unit) = cache.getOrPut(id){
            factory().apply {
                if(!loadById(id)){
                    default()
                }
            }
        }
    }
}