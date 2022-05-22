package cf.wayzer.scriptAgent

import cf.wayzer.scriptAgent.define.LoaderApi
import cf.wayzer.scriptAgent.define.ScriptState
import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
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
            ?.loadClass("cf.wayzer.scriptAgent.GenerateMain")
            ?.getDeclaredMethod("afterLoad", Array<String>::class.java)?.invoke(null, args)
            ?: exitProcess(-1)
    }

    @JvmStatic
    @Suppress("unused")
    fun afterLoad(args: Array<String>) {
        DependencyManager {
            addRepository("https://www.jitpack.io/")
            requireWithChildren(Dependency.parse("com.github.Anuken.MindustryJitpack:core:914fcd90a5"))
            loadToClassLoader(GenerateMain::class.java.classLoader)
        }

        Config.rootDir = File("scripts")
        ScriptRegistry.scanRoot()

        var notFound = 0
        if (args.isEmpty())
            ScriptManager.loadAll(false)
        else
            args.forEach {
                val script = ScriptManager.getScriptNullable(it)
                if (script == null) {
                    println("找不到脚本: $it")
                    notFound++
                    return@forEach
                }
                ScriptManager.loadScript(script, enable = false, children = false)
            }
        val fail = ScriptRegistry.allScripts { it.scriptState == ScriptState.Fail }.count()
        if (notFound != 0)
            println("有${notFound}个输入脚本未找到")
        println("共加载${ScriptRegistry.allScripts { it.scriptState != ScriptState.ToLoad }.size}个脚本，失败${fail}个")
        exitProcess(notFound + fail)
    }
}