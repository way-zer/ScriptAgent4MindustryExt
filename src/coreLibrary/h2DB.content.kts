package coreLibrary

import cf.wayzer.script_agent.Config
import org.jetbrains.exposed.sql.Database

onEnable {
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
    DataBaseApi.db.set(Database.connect("jdbc:h2:${file.absolutePath}", "org.h2.Driver"))
}