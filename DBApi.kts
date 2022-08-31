@file:Import("org.jetbrains.exposed:exposed-core:0.37.3", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-dao:0.37.3", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-java-time:0.37.3", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jdbc:0.37.3", mavenDepends = true)

package coreLibrary

import coreLibrary.lib.util.ServiceRegistry
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlin.system.measureTimeMillis


@Suppress("unused", "MemberVisibilityCanBePrivate")
object DB : ServiceRegistry<Database>() {
    object TableVersion : IdTable<String>("TableVersion") {
        override val id: Column<EntityID<String>> = text("table").entityId()
        val version = integer("version")
        val updateDate = timestamp("time")

        /**@return 0 if not exist */
        fun get(table: Table) = with(TransactionManager.current()) {
            select { TableVersion.id eq identity(table) }.firstOrNull()?.get(version) ?: 0
        }

        fun update(table: Table, versionV: Int) = with(TransactionManager.current()) {
            if (get(table) == 0)
                insert {
                    it[id] = identity(table)
                    it[version] = versionV
                    it[updateDate] = Instant.now()
                }.execute(this)
            else
                update({ TableVersion.id eq identity(table) }) {
                    it[version] = versionV
                    it[updateDate] = Instant.now()
                }
        }
    }

    private val key = DataKeyWithDefault("DB_registeredTable") { mutableSetOf<Table>() }
    private val Script.registeredTable by key

    interface WithUpgrade {
        val version: Int

        /**will be call in transaction*/
        fun onUpgrade(oldVersion: Int)
    }

    /**
     * 为模块注册表格
     * 注册时不一定立刻运行
     * 会等[DB]初始化后统一注册
     * 如果DB有版本变化,请实现[WithUpgrade]，未实现默认版本号1
     */
    @Synchronized
    fun Script.registerTable(vararg t: Table) {
        registeredTable.addAll(t)
        if (DB.provided)
            transaction {
                withDataBaseLock { initTable(t.asIterable()) }
            }
    }

    @Synchronized
    internal fun initDB(db: Database) {
        TransactionManager.defaultDatabase = db
        val allTable = ScriptManager.allScripts { it.inst?.dslExists(key) == true }
            .flatMapTo(mutableSetOf()) { it.inst!!.registeredTable }

        transaction {
            withDataBaseLock {
                SchemaUtils.create(TableVersion)
                initTable(allTable)
            }
        }
    }

    private fun initTable(tables0: Iterable<Table>) {
        val tables = SchemaUtils.sortTablesByReferences(tables0)
        val time = measureTimeMillis {
            tables.forEach {
                val version = (it as? WithUpgrade)?.version ?: 1
                val nowVersion = TableVersion.get(it)
                if (nowVersion < version) {
                    exposedLogger.info("Do Database upgrade for $it: $nowVersion -> $version")
                    if (it is WithUpgrade) it.onUpgrade(nowVersion)
                    else SchemaUtils.createMissingTablesAndColumns(it)
                    TableVersion.update(it, version)
                }
            }
        }
        exposedLogger.info("Finish check upgrade for ${tables.size} tables, costs $time ms")
    }
}

DB.subscribe(this) { DB.initDB(it) }