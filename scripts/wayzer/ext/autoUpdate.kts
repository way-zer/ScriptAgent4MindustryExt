package wayzer.ext

import arc.util.Http
import arc.util.Log
import arc.util.async.Threads
import arc.util.serialization.Jval
import mindustry.core.Version
import mindustry.gen.Groups
import mindustry.io.SaveIO
import mindustry.net.BeControl
import java.io.File
import java.net.URL
import kotlin.system.exitProcess

name = "自动更新"

val enableUpdate by config.key(false, "是否开启自动更新")

var updateCallback: (() -> Unit)? = null

onEnable {
    launch {
        while (updateCallback == null) {
            if (enableUpdate)
                Http.get("https://api.github.com/repos/Anuken/Mindustry/releases", { res ->
                    if (res.status == Http.HttpStatus.OK) {
                        val json = Jval.read(res.resultAsString).asArray().first()
                        var newBuild = json.getString("tag_name", "")
                        if (!newBuild.contains('.')) newBuild += ".0"
                        if (newBuild > ("v${Version.build}.${Version.revision}")) {
                            val asset = json.get("assets").asArray().find {
                                it.getString("name", "").startsWith("server-release")
                            }
                            val url = asset.getString("browser_download_url", "")
                            update(newBuild, "https://gh.api.99988866.xyz/$url")
                        }
                    }
                }, {
                    /*ignore error*/
                    logger.warning("获取更新数据失败:" + it.message)
                })
            delay(5 * 60_000)//延时5分钟
        }
    }
}

fun update(version: String, url: String) {
    launch(Dispatchers.IO) {
        Log.info("发现新版本可用 $version 正在从 $url 下载")
        val con = URL(url).openConnection()
        val dest = File(BeControl::class.java.protectionDomain.codeSource.location.toURI().path)
        val tmp = dest.resolveSibling("server-be-$version.jar")
        val size = suspendCancellableCoroutine<Long> { cont ->
            val steam = con.getInputStream()
            @OptIn(ExperimentalCoroutinesApi::class)
            cont.resume(steam.use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }) {
                steam.close()
            }
        }
        Log.info("新版本 $version 下载完成: ${size / 1024 / 1024}MB")
        updateCallback = {
            Groups.player.forEach {
                it.kick("[yellow]服务器重启更新到新版本 $version")
            }
            Threads.sleep(32L)
            Log.info("&lcVersion downloaded, exiting. Note that if you are not using a auto-restart script, the server will not restart automatically.")
            tmp.renameTo(dest)
            exitProcess(2)
        }
        Core.app.post {
            broadcast("[yellow]服务器新版本{version}下载完成,将在本局游戏后自动重启更新".with("version" to version))
            if (Groups.player.isEmpty) {
                Log.info("&lcSaving...")
                @Suppress("SpellCheckingInspection")
                SaveIO.save(saveDirectory.child("autosavebe.$saveExtension"))
                Log.info("&lcAutoSaved.")
                updateCallback!!()
            }
        }
    }
}

listen<EventType.WorldLoadEvent> {
    updateCallback?.invoke()
}