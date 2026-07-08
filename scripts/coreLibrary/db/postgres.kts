@file:Import("org.postgresql:postgresql:42.7.5", mavenDepends = true)

import cf.wayzer.scriptAgent.util.Services
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ExperimentalKeywordApi

val url by config.key("postgresql://db:5432/postgres", "数据库连接uri")
val user by config.key("", "用户名")
val password by config.key("", "密码")
val preserveKeywordCasing by config.key(true, "是否保留关键字大小写, 老用户请设置为false")
val preserveKeywordCasing0 get() = preserveKeywordCasing

onEnable {
    if (user.isEmpty() || password.isEmpty()) {
        ScriptManager.disableScript(this, "配置无效，请先通过sa config设置user/password.")
        return@onEnable
    }

    Class.forName("org.postgresql.Driver")
    val db = Database.connect({
        java.sql.DriverManager.getConnection("jdbc:$url", user, password)
    }, DatabaseConfig {
        @OptIn(ExperimentalKeywordApi::class)
        preserveKeywordCasing = preserveKeywordCasing0
    })
    Services.provide(db)
}