@file:Depends("coreLibrary/DBApi")

package coreLibrary

import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ExperimentalKeywordApi
import java.sql.DriverManager

val driverMaven by config.key("com.h2database:h2:2.0.206", "驱动程序maven包")
val driver by config.key("org.h2.Driver", "驱动程序类名")
val url by config.key("jdbc:h2:H2DB_PATH", "数据库连接uri", "特殊变量H2DB_PATH 指向data/h2DB.db")
val user by config.key("", "用户名")
val password by config.key("", "密码")
val preserveKeywordCasing by config.key(true, "是否保留关键字大小写, 老用户请设置为false")

//Postgres example
// driverMaven: org.postgresql:postgresql:42.7.5
// driver: org.postgresql.Driver
// url: jdbc:postgresql://db:5432/postgres
// user: postgres
// password: your_password

onEnable {
    DependencyManager {
        require(Dependency.parse(driverMaven))
        loadToClassLoader(thisScript.javaClass.classLoader)
    }
    Class.forName(driver)

    val url = url.replace("H2DB_PATH", Config.dataDir.resolve("h2DB.db").absolutePath)
    val db = Database.connect({
        DriverManager.getConnection(url, user, password)
    }, DatabaseConfig {
        @OptIn(ExperimentalKeywordApi::class)
        preserveKeywordCasing = thisScript.preserveKeywordCasing
    })
    DBApi.DB.provide(this, db)
}