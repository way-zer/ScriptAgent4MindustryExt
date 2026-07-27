package coreLib.db

import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.util.DSLBuilder
import cf.wayzer.scriptAgent.util.Services
import coreLibrary.lib.getOrNull
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlin.system.measureTimeMillis

object DBApi {
    val db = Services.get<Database>()

    private var Script.registeredTable: List<Table>? by DSLBuilder.dataKey()

    object TableVersion : IdTable<String>("TableVersion") {
        // can't use `text` as h2db don't support for primaryKey

        override val id: Column<EntityID<String>> = varchar("table", 64).entityId()
        override val primaryKey: PrimaryKey = PrimaryKey(id) // h2database#2191

        val version = integer("version")
        val updateDate = timestamp("time")

        /**@return 0 if not exist */
        fun get(table: Table): Int {
            val identity = TransactionManager.current().identity(table)
            return select(version).where { id eq identity }.firstOrNull()?.get(version) ?: 0
        }

        fun update(table: Table, versionV: Int) {
            val identity = TransactionManager.current().identity(table)
            if (get(table) == 0)
                insert {
                    it[id] = identity
                    it[version] = versionV
                    it[updateDate] = Instant.now()
                }
            else
                update({ id eq identity }) {
                    it[version] = versionV
                    it[updateDate] = Instant.now()
                }
        }

        fun check(table: Table) {
            val version = (table as? WithUpgrade)?.version ?: 1
            val nowVersion = get(table)
            if (nowVersion < version) {
                exposedLogger.info("Do Database upgrade for $table: $nowVersion -> $version")
                try {
                    if (table is WithUpgrade) table.onUpgrade(nowVersion)
                    else SchemaUtils.createMissingTablesAndColumns(table)

                    update(table, version)
                } catch (e: Throwable) {
                    exposedLogger.error("Fail to do Database upgrade for $table: $nowVersion -> $version", e)
                }
            }
        }
    }

    interface WithUpgrade {
        val version: Int

        /**will be call in transaction*/
        fun onUpgrade(oldVersion: Int) {
            SchemaUtils.createMissingTablesAndColumns(this as Table)
        }
    }

    /**
     * 为模块注册表格
     * 注册时不一定立刻运行
     * 会等[db]初始化后统一注册
     * 如果DB有版本变化,请实现[WithUpgrade]，未实现默认版本号1
     */
    context(script: Script)
    fun registerTable(vararg t: Table) {
        script.registeredTable = script.registeredTable.orEmpty() + t
        db.getOrNull()?.let {
            transaction(it) {
                withDataBaseLock { initTable(t.asIterable()) }
            }
        }
    }

    @Synchronized
    fun initDB(db: Database) {
        TransactionManager.defaultDatabase = db
        val allTable = ScriptRegistry.allScripts { it.inst != null }
            .flatMapTo(mutableSetOf()) { it.inst?.registeredTable.orEmpty() }

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
            tables.forEach { TableVersion.check(it) }
        }
        exposedLogger.info("Finish check upgrade for ${tables.size} tables, costs $time ms")
    }
}