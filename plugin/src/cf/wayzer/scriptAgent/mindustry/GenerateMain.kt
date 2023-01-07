package cf.wayzer.scriptAgent.mindustry

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.ScriptAgent
import cf.wayzer.scriptAgent.ScriptManager
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.define.LoaderApi
import cf.wayzer.scriptAgent.define.ScriptState
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

@OptIn(LoaderApi::class)
object GenerateMain {
    @JvmStatic
    fun main(args: Array<String>?) {
        if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
            System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n"
            )
        ScriptAgent.loadUseClassLoader()
            ?.loadClass(GenerateMain::class.java.name)
            ?.getDeclaredMethod("afterLoad", Array<String>::class.java)?.invoke(null, args)
            ?: exitProcess(-1)
    }

    @JvmStatic
    @Suppress("unused")
    fun afterLoad(args: Array<String>) {
        DependencyManager {
            addRepository("https://www.jitpack.io/")
            requireWithChildren(Dependency.parse("com.github.TinyLake.MindustryX:core:v140.101"))
            loadToClassLoader(GenerateMain::class.java.classLoader)
        }

        Config.rootDir = File("scripts")
        ScriptRegistry.scanRoot()

        runBlocking {
            ScriptManager.transaction {
                if (args.isEmpty())
                    addAll()
                else
                    args.forEach { add(it) }
                load()
            }
        }
        val fail = ScriptRegistry.allScripts { it.failReason != null }
        println(
            "共加载${ScriptRegistry.allScripts { it.scriptState != ScriptState.Found }.size}个脚本，失败${fail.size}个"
        )
        fail.forEach {
            println("\t${it.id}: ${it.failReason}")
        }
        exitProcess(fail.size)
    }
}