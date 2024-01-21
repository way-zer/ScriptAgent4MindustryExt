package bootStrap

import cf.wayzer.scriptAgent.util.DependencyManager
import cf.wayzer.scriptAgent.util.maven.Dependency
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

fun prepareBuiltin(outputDir: File = File("build/tmp/builtinScripts")) {
    val toSave = ScriptRegistry.allScripts { it.scriptState.loaded }
        .mapNotNull { it.compiledScript }
        .sortedBy { it.id }
    println("prepare Builtin for ${toSave.size} scripts.")

    val md5Digest = MessageDigest.getInstance("MD5")
    val destCAS = outputDir.resolve("by_md5").also(File::mkdirs)
    fun addCAS(bs: ByteArray): String {
        val md5 = BigInteger(1, md5Digest.digest(bs)).toString(16)
        destCAS.resolve(md5).writeBytes(bs)
        return md5
    }

    val meta = mutableListOf<String>()
    toSave.forEach { script ->
        val idWithModule = script.source.run { if (isModule) id + Config.moduleIdSuffix else id }
        val scriptMD5 = addCAS(script.compiledFile.readBytes())

        val resources = script.source.listResources()
            .map { it.name to addCAS(it.loadFile().readBytes()) }
            .sortedBy { it.first }
            .joinToString(";") { "${it.first}:${it.second}" }
        meta.add("$idWithModule $scriptMD5 $resources")
    }
    outputDir.resolve("META").bufferedWriter().use {
        meta.joinTo(it, "\n")
    }
}

fun prepareScripts(outputDir: File = File("build/tmp/scripts")) {
    val toSave = ScriptRegistry.allScripts { it.scriptState.loaded }
        .mapNotNull { it.compiledScript }
        .sortedBy { it.id }
    println("prepare scripts for ${toSave.size} scripts.")

    toSave.forEach { script ->
        val ktcFile = script.compiledFile
        ktcFile.copyTo(outputDir.resolve(ktcFile.relativeTo(Config.cacheDir)).also { it.parentFile.mkdirs() })

        script.source.listResources().forEach { res ->
            val file = res.loadFile()
            file.copyTo(outputDir.resolve(file.relativeTo(Config.rootDir)).also { it.parentFile.mkdirs() })
        }
    }
}

onEnable {
    if (id != Config.mainScript)
        return@onEnable ScriptManager.disableScript(this, "仅可通过SAMAIN启用")
    DependencyManager {
        addRepository("https://www.jitpack.io/")
        requireWithChildren(Dependency.parse("com.github.TinyLake.MindustryX_tmp:core:v145.103"))
        loadToClassLoader(ScriptAgent::class.java.classLoader)
    }
    ScriptManager.transaction {
        if (Config.args.isEmpty())
            addAll()
        else
            Config.args.forEach { add(it) }
        load()
    }
    val fail = ScriptRegistry.allScripts { it.failReason != null }
    println("共加载${ScriptRegistry.allScripts { it.scriptState != ScriptState.Found }.size}个脚本，失败${fail.size}个")
    fail.forEach {
        println("\t${it.id}: ${it.failReason}")
    }
    if (System.getProperty("ScriptAgent.PreparePack") != null) {
        println("Finish prepareScripts in ${measureTimeMillis { prepareScripts() }}ms")
        println("Finish prepareBuiltin in ${measureTimeMillis { prepareBuiltin() }}ms")
    }
    exitProcess(fail.size)
}