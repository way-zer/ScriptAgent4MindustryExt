@file:Import("com.h2database:h2:2.0.206", mavenDepends = true)

import cf.wayzer.scriptAgent.util.Services
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.ExperimentalKeywordApi
import java.sql.DriverManager

val enable by config.key(true, "开启H2DB")
val preserveKeywordCasing by config.key(true, "是否保留关键字大小写, 老用户请设置为false")
val preserveKeywordCasing0 get() = preserveKeywordCasing

onEnable {
    if (!enable) {
        ScriptManager.disableScript(this, "配置关闭")
        return@onEnable
    }
    Class.forName("org.h2.Driver")

    val path = Config.dataDir.resolve("h2DB.db").absolutePath
    val db = Database.connect({
        DriverManager.getConnection("jdbc:h2:$path")
    }, DatabaseConfig {
        @OptIn(ExperimentalKeywordApi::class)
        preserveKeywordCasing = preserveKeywordCasing0
    })
    @OptIn(SAExperimentalApi::class)
    Services.provide(db)
}