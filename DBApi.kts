@file:Import("org.jetbrains.exposed:exposed-core:0.37.3", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-dao:0.37.3", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-java-time:0.37.3", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jdbc:0.37.3", mavenDepends = true)

package coreLibrary

import coreLibrary.lib.util.ServiceRegistry
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction


@Suppress("unused")
object DB : ServiceRegistry<Database>() {
    private val Script.registeredTable by dataKeyWithDefault { mutableSetOf<Table>() }

    /**
     * 为模块注册表格
     * 注册时不一定立刻运行
     * 会等[DB]初始化后统一注册
     */
    fun Script.registerTable(vararg t: Table) {
        registeredTable.addAll(t)
        subscribe(this) {
            transaction(it) {
                SchemaUtils.createMissingTablesAndColumns(*t)
            }
        }
    }
}

DB.subscribe(this) {
    TransactionManager.defaultDatabase = it
}