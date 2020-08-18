//1. 不同数据库的驱动Maven,根据选择注释
@file:MavenDepends("com.h2database:h2:1.4.200", single = false)
//@file:MavenDepends("org.postgresql:postgresql:42.2.15", single = false)
@file:Suppress("unused")

package coreLibrary

import cf.wayzer.script_agent.Config
import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import java.sql.DriverManager

//2. 修改对应类型中需要配置的项(地址，用户名，密码)
fun h2(): () -> Connection {
    sourceFile.parentFile.listFiles{_,n->n.startsWith("h2DB.db")}?.takeIf { it.isNotEmpty() }?.forEach {
        println("检测到旧数据库文件,自动迁移到新目录")
        val new = Config.dataDirectory.resolve(it.name)
        if(new.exists()){
            println("目标文件$new 存在,不进行覆盖，请自行处理")
        }else {
            it.copyTo(new)
            it.delete()
        }
    }
    val file = Config.dataDirectory.resolve("h2DB.db")
    Class.forName("org.h2.Driver")
    return {DriverManager.getConnection("jdbc:h2:${file.absolutePath}")}
}

fun postgre(): () -> Connection {
    Class.forName("org.postgresql.Driver")
    //使用请修改此处连接方式与账号密码
    return {DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindustry","mindustry", "")}
}

onEnable {
    //3. 请重新注释此处
    DataBaseApi.db.set(Database.connect(h2()))
//    DataBaseApi.db.set(Database.connect(postgre()))
}