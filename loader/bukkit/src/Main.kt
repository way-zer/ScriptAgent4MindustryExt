package cf.wayzer.scriptAgent.bukkit

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptManager
import cf.wayzer.scriptAgent.define.LoaderApi
import cf.wayzer.scriptAgent.util.CommonMain
import cf.wayzer.scriptAgent.util.DSLBuilder
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import kotlinx.coroutines.runBlocking
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin

@OptIn(LoaderApi::class)
class Main : JavaPlugin(), CommonMain {
    var Config.pluginMain by DSLBuilder.dataKey<JavaPlugin>()
    var Config.pluginCommand by DSLBuilder.dataKey<PluginCommand>()
    val Config.delayEnable by DSLBuilder.dataKeyWithDefault { mutableListOf<Runnable>() }

    override fun onLoad() {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        initConfigInfo(dataFolder, pluginMeta.version)
        Config.libraryDir = Config.cacheDir.resolve("libs").toPath()
        Config.logger = logger
        Config.pluginMain = this

        DependencyManager {
            require(Dependency.parse("org.jetbrains.kotlin:kotlin-stdlib:${Config.kotlinVersion}"))
            require(Dependency.parse("org.jetbrains.kotlin:kotlin-reflect:${Config.kotlinVersion}"))
            require(Dependency.parse("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Config.kotlinCoroutineVersion}"))
            addAsGlobal()
        }

        bootstrap()
    }

    override fun onEnable() {
        Config.pluginCommand = getCommand("ScriptAgent")
        Config.delayEnable.toList().let { list ->
            Config.delayEnable.clear()
            list.forEach { it.run() }
        }
    }

    override fun onDisable() {
        runBlocking {
            ScriptManager.disableAll()
        }
    }
}