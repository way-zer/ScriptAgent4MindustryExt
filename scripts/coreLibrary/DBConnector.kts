@file:Depends("coreLibrary/DBApi")

package coreLibrary

import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import org.jetbrains.exposed.sql.Database
import java.sql.DriverManager

val driverMaven by config.key("com.h2database:h2:1.4.200", "驱动程序maven包")
val driver by config.key("org.h2.Driver", "驱动程序类名")
val url by config.key("jdbc:h2:H2DB_PATH", "数据库连接uri", "特殊变量H2DB_PATH 指向data/h2DB.db")
val user by config.key("", "用户名")
val password by config.key("", "密码")

//Postgres example
// driverMaven: org.postgresql:postgresql:42.2.15
// driver: org.postgresql.Driver
// url: jdbc:postgresql://db:5432/postgres
// user: postgres
// password: your_password

onEnable {
    val cClassloader = javaClass.classLoader!!
    DependencyManager {
        requireWithChildren(Dependency.parse(driverMaven))
        loadToClassLoader(cClassloader)
    }
    Class.forName(driver)

    val url = url.replace("H2DB_PATH", Config.dataDir.resolve("h2DB.db").absolutePath)
    DBApi.DB.provide(this, Database.connect({
        DriverManager.getConnection(url, user, password)
    }))
}