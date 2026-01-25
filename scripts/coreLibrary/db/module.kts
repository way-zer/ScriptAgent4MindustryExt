@file:Depends("coreLibrary")
@file:Import("org.jetbrains.exposed:exposed-core:0.59.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-dao:0.59.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-java-time:0.59.0", mavenDepends = true)
@file:Import("org.jetbrains.exposed:exposed-jdbc:0.59.0", mavenDepends = true)

import coreLib.db.DBApi
import java.util.logging.Level

onEnable {
    launch {
        DBApi.db.observe().collect {
            if (it.size > 1) {
                logger.warning("More than one database registered: $it")
            }
            val db = it.firstOrNull() ?: return@collect
            try {
                DBApi.initDB(db)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error when initDB", e)
            }
        }
    }
}