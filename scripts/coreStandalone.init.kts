@file:DependsModule("coreLibrary")

import coreStandalone.lib.RootCommands
import java.io.File

name = "core module for standalone"
addDefaultImport("coreStandalone.lib.*")
generateHelper()
ICommands.rootProvider.set(RootCommands)

onEnable {
    File("data").mkdirs()
    ConfigBuilder.init(File("data/webConfig.conf"))
    DataStoreApi.open("data/webDatabase.db")
}