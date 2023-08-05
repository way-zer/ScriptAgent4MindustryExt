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
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

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
            requireWithChildren(Dependency.parse("com.github.TinyLake.MindustryX_tmp:core:v145.103"))
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
        if (System.getProperty("ScriptAgent.PreparePack") != null) {
            val time = measureTimeMillis { preparePack() }
            println("Finish preparePack in ${time}ms")
        }
        exitProcess(fail.size)
    }

    private fun preparePack() {
        val destScripts = File("build/tmp/scripts")
        val destBuiltin = File("build/tmp/builtinScripts")
        val toSave = ScriptRegistry.allScripts { it.scriptState.loaded }
            .mapNotNull { it.compiledScript }
            .sortedBy { it.id }
        println("prepare Pack for ${toSave.size} scripts.")

        val md5Digest = MessageDigest.getInstance("MD5")
        val destCAS = destBuiltin.resolve("by_md5").also { it.mkdirs() }
        fun addCAS(bs: ByteArray): String {
            val md5 = BigInteger(1, md5Digest.digest(bs)).toString(16)
            destCAS.resolve(md5).writeBytes(bs)
            return md5
        }

        val meta = mutableListOf<String>()
        toSave.forEach { script ->
            val idWithModule = script.source.run { if (isModule) id + Config.moduleIdSuffix else id }
            val ktcFile = script.compiledFile
            ktcFile.copyTo(destScripts.resolve(ktcFile.relativeTo(Config.cacheDir)).also { it.parentFile.mkdirs() })
            val scriptMD5 = addCAS(ktcFile.readBytes())

            val resources = script.source.listResources().map { res ->
                val file = res.loadFile()
                file.copyTo(destScripts.resolve(file.relativeTo(Config.rootDir)).also { it.parentFile.mkdirs() })
                res.name to addCAS(file.readBytes())
            }.sortedBy { it.first }.joinToString(";") { "${it.first}:${it.second}" }
            meta.add("$idWithModule $scriptMD5 $resources")
        }
        destBuiltin.resolve("META").bufferedWriter().use {
            meta.joinTo(it, "\n")
        }
    }
}