@file:Import("org.jetbrains.exposed:exposed-core:0.29.1", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-dao:0.29.1", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-java-time:0.29.1", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jdbc:0.29.1", mavenDepends = true)

package coreLibrary

import coreLibrary.lib.util.Provider
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction


object DB {
    private val ISubScript.registeredTable by dataKeyWithDefault { mutableSetOf<Table>() }
    val db = Provider<Database>()

    /**
     * 为模块注册表格
     * 注册时不一定立刻运行
     * 会等[db]初始化后统一注册
     */
    fun ISubScript.registerTable(vararg t: Table) {
        registeredTable.addAll(t)
        db.listenWithAutoCancel(this) {
            transaction(it) {
                SchemaUtils.createMissingTablesAndColumns(*t)
            }
        }
    }
}

DB.db.every {
    TransactionManager.defaultDatabase = it
}