@file:DependsModule("coreLibrary")

import coreStandalone.lib.RootCommands

name = "core module for standalone"
addDefaultImport("coreStandalone.lib.*")
generateHelper()
Commands.rootProvider.set(RootCommands)