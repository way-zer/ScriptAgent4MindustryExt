@file:Depends("coreLibrary/DBApi")
//1. 不同数据库的驱动Maven,根据选择注释
@file:Import("com.h2database:h2:1.4.200", mavenDepends = true)
//@file:Import("org.postgresql:postgresql:42.2.15", mavenDepends = true)
//@file:Import("mysql:mysql-connector-java:8.0.28", mavenDepends = true)
@file:Suppress("unused")

package coreLibrary

import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import java.sql.DriverManager

//2. 修改对应类型中需要配置的项(地址，用户名，密码)
fun h2(): () -> Connection {
    sourceFile.parentFile.listFiles { _, n -> n.startsWith("h2DB.db") }?.takeIf { it.isNotEmpty() }?.forEach {
        logger.info("检测到旧数据库文件,自动迁移到新目录")
        val new = Config.dataDir.resolve(it.name)
        if (new.exists()) {
            logger.warning("目标文件$new 存在,不进行覆盖，请自行处理")
        } else {
            it.copyTo(new)
            it.delete()
        }
    }
    val file = Config.dataDir.resolve("h2DB.db")
    Class.forName("org.h2.Driver")
    return { DriverManager.getConnection("jdbc:h2:${file.absolutePath}") }
}

fun postgre(): () -> Connection {
    Class.forName("org.postgresql.Driver")
    //使用请修改此处连接方式与账号密码
    return { DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindustry", "mindustry", "") }
}

fun mysql(): () -> Connection {
    //如果您使用的是Mysql 5.7+ 那么JDBC推荐8.x
    // JDBC为 8.x时
    Class.forName("com.mysql.cj.jdbc.Driver")
    // JDBC为 8.x以下时
    //Class.forName("com.mysql.jdbc.Driver")

    //使用请修改此处连接方式与账号密码
    return { DriverManager.getConnection("jdbc:mysql://localhost:3306/mindustry", "mindustry", "") }
}

onEnable {
    //3. 请重新注释此处
    DBApi.DB.provide(this, Database.connect(h2()))
//    DBApi.DB.provide(this, Database.connect(postgre()))
//    DBApi.DB.provide(this, Database.connect(mysql()))
}