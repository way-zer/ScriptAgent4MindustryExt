package coreLibrary

import org.jetbrains.exposed.sql.Database

onEnable {
    val file = sourceFile.parentFile.resolve("h2DB.db")
    DataBaseApi.db.set(Database.connect("jdbc:h2:${file.absolutePath}", "org.h2.Driver"))
}