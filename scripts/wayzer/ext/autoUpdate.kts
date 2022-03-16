package wayzer.ext

import arc.util.Http
import arc.util.Log
import arc.util.async.Threads
import arc.util.serialization.Jval
import mindustry.core.Version
import mindustry.gen.Groups
import mindustry.net.BeControl
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

name = "自动更新"

val enableUpdate by config.key(false, "是否开启自动更新")
val source by config.key("Anuken/Mindustry", "服务端来源，Github仓库")

var updateCallback: (() -> Unit)? = null

suspend fun http(url: String) = suspendCoroutine<Http.HttpResponse> {
    Http.get(url, it::resume, it::resumeWithException)
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

onEnable {
    launch {
        while (true) {
            if (enableUpdate) {
                try {
                    val res = http("https://api.github.com/repos/$source/releases")
                    if (res.status == Http.HttpStatus.OK) {
                        val json = Jval.read(res.resultAsString).asArray().first()
                        val newBuild = json.getString("tag_name", "")
                        val (version, revision) = ("$newBuild.0").split(".").map { it.toInt() }
                        if (version > Version.build || revision > Version.revision) {
                            val asset = json.get("assets").asArray().find {
                                it.getString("name", "").startsWith("server-release")
                            }
                            val url = asset.getString("browser_download_url", "")
                            try {
                                update(newBuild, "https://gh.api.99988866.xyz/$url")
                                break
                            } catch (e: Throwable) {
                                logger.warning("下载更新失败: " + e.message)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    logger.warning("获取更新数据失败:" + e.message)
                }
            }
            delay(5 * 60_000)//延时5分钟
        }
    }
}

suspend fun update(version: String, url: String) {
    Log.info("发现新版本可用 $version 正在从 $url 下载")
    val dest = File(BeControl::class.java.protectionDomain.codeSource.location.toURI().path)
    val tmp = dest.resolveSibling("server-$version.jar.tmp")
    val size = try {
        download(url, tmp)
    } catch (e: Throwable) {
        tmp.delete()
        throw e
    }
    Log.info("新版本 $version 下载完成: ${size / 1024 / 1024}MB")
    updateCallback = {
        Groups.player.forEach {
            it.kick("[yellow]服务器重启更新到新版本 $version")
        }
        Threads.sleep(32L)
        Log.info("&lcVersion downloaded, exiting. Note that if you are not using a auto-restart script, the server will not restart automatically.")
        tmp.copyTo(dest, true)
        tmp.delete()
        exitProcess(2)
    }
    broadcast("[yellow]服务器新版本{version}下载完成,将在本局游戏后自动重启更新".with("version" to version))
}

listen<EventType.WorldLoadEvent> {
    updateCallback?.invoke()
}