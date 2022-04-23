package wayzer.ext

import arc.util.Interval
import arc.util.Log
import arc.util.async.Threads
import arc.util.serialization.Jval
import mindustry.core.Version
import mindustry.gen.Groups
import mindustry.net.BeControl
import java.io.File
import java.net.URL
import kotlin.system.exitProcess

name = "自动更新"

val enableUpdate by config.key(false, "是否开启自动更新")
val source by config.key("Anuken/Mindustry", "服务端来源，Github仓库")

var updateCallback: (() -> Unit)? = null

suspend fun download(url: String, file: File): Int = withContext(Dispatchers.IO) {
    val steam = URL(url).openStream()
    val buffer = ByteArray(128 * 1024)//128KB
    val logInterval = Interval()
    var len = 0
    steam.use { input ->
        file.outputStream().use { output ->
            while (isActive) {
                val i = input.read(buffer)
                if (i == -1) break
                output.write(buffer, 0, i)
                len += i
                if (logInterval[60f])
                    logger.info("Downloaded ${len / 1024}KB")
            }
        }
    }
    return@withContext len
}

onEnable {
    launch {
        while (true) {
            if (enableUpdate) {
                try {
                    val txt = URL("https://api.github.com/repos/$source/releases").readText()
                    val json = Jval.read(txt).asArray().first()
                    val newBuild = json.getString("tag_name", "")
                    val (version, revision) = ("$newBuild.0").removePrefix("v")
                        .split(".").map { it.toInt() }
                    if (version > Version.build || revision > Version.revision) {
                        val asset = json.get("assets").asArray().find {
                            it.getString("name", "").startsWith("server-release")
                        }
                        val url = asset.getString("browser_download_url", "")
                        try {
                            update(newBuild, "https://gh.tinylake.cf/$url")
                            break
                        } catch (e: Throwable) {
                            logger.warning("下载更新失败: " + e.message)
                            e.printStackTrace()
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
    Log.info("新版本 $version 下载完成: ${size / 1024}KB")
    updateCallback = {
        Groups.player.forEach {
            it.kick("[yellow]服务器重启更新到新版本 $version")
        }
        Threads.sleep(32L)
        dest.outputStream().use { output ->
            tmp.inputStream().use { it.copyTo(output) }
        }
        tmp.delete()
        Log.info("&lcVersion downloaded, exiting. Note that if you are not using a auto-restart script, the server will not restart automatically.")
        exitProcess(2)
    }
    broadcast("[yellow]服务器新版本{version}下载完成,将在本局游戏后自动重启更新".with("version" to version))
}

listen<EventType.ResetEvent> {
    updateCallback?.invoke()
}