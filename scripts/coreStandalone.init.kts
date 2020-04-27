@file:DependsModule("coreLibrary")

import java.io.File

name = "core module for standalone"
addDefaultImport("coreStandalone.lib.*")
generateHelper()

onEnable {
    ConfigBuilder.init(File("data/webConfig.conf"))
    DataStoreApi.open("data/webDatabase.db")
}