@file:Depends("coreLibrary")
@file:Import("arc.Core", libraryByClass = true, env="mindustry")
@file:Import("mindustry.Vars", libraryByClass = true, env="mindustry")
@file:Import("https://www.jitpack.io/", mavenRepository = true, env="!mindustry")
@file:Import("com.github.TinyLake.MindustryX:core:v2026.10.X38", mavenDepends = true, env="!mindustry")
@file:Import("arc.Core", defaultImport = true)
@file:Import("mindustry.Vars.*", defaultImport = true)
@file:Import("mindustry.content.*", defaultImport = true)
@file:Import("mindustry.gen.Player", defaultImport = true)
@file:Import("mindustry.gen.Call", defaultImport = true)
@file:Import("mindustry.gen.Groups", defaultImport = true)
@file:Import("mindustry.game.EventType", defaultImport = true)
@file:Import("coreMindustry.lib.*", defaultImport = true)

package coreMindustry

Listener//ensure init
onEnable {
    RootCommands.hookGameHandler()
}
