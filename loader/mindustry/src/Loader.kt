package cf.wayzer.scriptAgent.mindustry

import arc.util.CommandHandler
import cf.wayzer.scriptAgent.*
import cf.wayzer.scriptAgent.define.LoaderApi
import mindustry.mod.Plugin

@Suppress("unused")//ref by plugin.json
class Loader : Plugin() {
    private val impl: Plugin

    init {
        if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
            System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n"
            )
        @OptIn(LoaderApi::class)
        impl = ScriptAgent.loadUseClassLoader()
            ?.loadClass(Main::class.java.name)
            ?.getConstructor(Plugin::class.java)
            ?.newInstance(this) as Plugin?
            ?: error("Fail newInstance")
    }

    override fun registerClientCommands(handler: CommandHandler?) {
        impl.registerClientCommands(handler)
    }

    override fun registerServerCommands(handler: CommandHandler?) {
        impl.registerServerCommands(handler)
    }

    override fun init() {
        impl.init()
    }
}