package main

import arc.util.Http
import arc.util.Log
import arc.util.serialization.Jval
import coreLibrary.lib.config
import coreLibrary.lib.util.loop
import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import coreMindustry.lib.command
import mindustry.net.BeControl
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

name = "自动更新"

val enableUpdate by config.key(true, "是否开启自动更新")
val source by config.key("Cong0707/ScriptAgent4MindustryExt", "服务端来源，Github仓库")

fun root(path: String): String {
    val roots: String = BeControl::class.java.protectionDomain.codeSource.location.toURI().path
    val rootsMinus = roots.replace("server-release.jar", "config/scripts/")
    return "$rootsMinus$path.kts"
}

suspend fun download(url: String, file: File): Long = withContext(Dispatchers.IO) {
    val con = URL(url).openConnection()
    val steam = con.getInputStream()
    coroutineContext.job.invokeOnCompletion { steam.close() }
    return@withContext steam.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

suspend fun update(version: String, url: String, path: String, script: String) {
    broadcast("[sky]发现新脚本可用 $version \n正在从 $url 下载".with())
    val dest = File(BeControl::class.java.protectionDomain.codeSource.location.toURI().path)
    val tmp = dest.resolveSibling("$version.kts.tmp")
    val size = try {
        download(url, tmp)
    } catch (e: Throwable) {
        tmp.delete()
        throw e
    }
    val pathFile = File(path)
    broadcast("[yellow]新版本 $version 下载完成: ${size}B".with())
    /*Groups.player.forEach {
            it.kick("[yellow]服务器重启更新到新版本 $version")
        }*/
    //Threads.sleep(32L)
    tmp.copyTo(pathFile, true)
    tmp.delete()
    //exitProcess(2)
    broadcast("[acid]新脚本可用:[sky]$script".with("version" to version))
}

var releaseID = 83636592

suspend fun http(url: String) = suspendCoroutine<Http.HttpResponse> {
    Http.get(url, it::resume, it::resumeWithException)
}

onEnable {
    loop {
        if (enableUpdate) {
            try {
                val urls = "https://api.github.com/repos/$source/releases"
                val txt = URL(urls).readText()
                val status = http(urls).status
                val ghproxyStatus = http(urls).status
                //broadcast("$status".with())
                //broadcast("$ghproxyStatus".with())
                val headers = http(urls).headers
                val ghproxyHeaders = http(urls).headers
                //broadcast("$headers".with())
                //broadcast("$ghproxyHeaders".with())
                val json = Jval.read(txt).asArray().first()
                val id = json.getString("id", "")
                val time = json.getString("published_at", "")
                if (id.toInt() >= releaseID) {
                    broadcast("[acid]检测到有新脚本,开始自动更新".with())
                    val asset = json.get("assets").asArray().find {
                        it.getString("name", "").endsWith("kts")
                    }
                    val path = json.getString("name", "")
                    val fullPath = root(path = path)
                    val url = asset.getString("browser_download_url", "")
                    try {
                        update(id, "http://ghproxy.com/$url", fullPath, path)
                        releaseID = id.toInt()
                        cancel()
                    } catch (e: Throwable) {
                        val msg = e.message
                        broadcast("下载更新失败:$msg".with())
                    }
                }
            } catch (e: Throwable) {
                broadcast("获取更新数据失败: $e".with())
            }
            Log.info("开始延时")
            delay(10 * 60 * 1000L)//延时10分钟
            Log.info("结束延时")
        }
    }
}

command("uptest", "强制更新服务器版本") {
    permission = dotId
    usage = "<url> <path>"
    body {
        arg.firstOrNull()?.let { kotlin.runCatching { URL(it) }.getOrNull() } ?: returnReply("请输入url".with())
        val path:String = arg.getOrNull(1)?: returnReply("请输入path".with())
        val pathAdd = root(path)
        reply("[green]正在后台处理中".with())
        launch {
            try {
                update("管理员手动升级", arg.first(), path = pathAdd, path)
            } catch (e: Throwable) {
                reply("[red]升级失败{e}".with("e" to e))
                e.printStackTrace()
            }
        }
    }
}
















