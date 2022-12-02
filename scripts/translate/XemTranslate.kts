@file:Depends("translate/baidu/BaiduFanyi", "调用翻译模块")

package translate

import arc.util.Log
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import coreMindustry.lib.command
import coreMindustry.lib.listen
import mindustry.game.EventType
import mindustry.net.BeControl
import translate.baidu.BaiduFanyi
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.util.function.Consumer

val baiduFanyi = contextScript<BaiduFanyi>()


var enable = false
var instance: XemTranslate? = null

Log.info("Loading XemTranslate main scripts.")

object ConfigUtils {

    private fun json(): File {
        val roots: String = BeControl::class.java.protectionDomain.codeSource.location.toURI().path
        val root = roots.replace("server-release.jar", "config/scripts/translate/config.json")
        return File(root)
    }

    operator fun set(path: String?, data: Any) {
        try {
            val jStr: String = json().readText()
            if (jStr.isNotEmpty()) {
                val jr = JsonReader()
                val json = jr.parse(jStr)
                var has = false
                for (v in json) {
                    if (v.name().equals(path, ignoreCase = true)) {
                        has = true
                        if (data is List<*>) {
                            val value = JsonValue(JsonValue.ValueType.array)
                            data.forEach { _ ->
                                value.addChild(
                                    JsonValue(
                                        data.toString()
                                    )
                                )
                            }
                            json.remove(path)
                            json.addChild(path, value)
                        } else v.set(data.toString())
                        break
                    }
                }
                if (!has) {
                    if (data is List<*>) {
                        val value = JsonValue(JsonValue.ValueType.array)
                        data.forEach { _ ->
                            value.addChild(
                                JsonValue(
                                    data.toString()
                                )
                            )
                        }
                        json.addChild(path, value)
                    } else json.addChild(path, JsonValue(data.toString()))
                }
                json().writeText(json.toJson(JsonWriter.OutputType.json))
            } else {
                val sw = StringWriter()
                val jw = JsonWriter(sw).`object`()
                if (data is List<*>) {
                    val arr = jw.array(path)
                    data.forEach { _ ->
                        try {
                            arr.value(data.toString())
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                    }
                } else jw[path] = data
                jw.close()
                json().writeText(sw.toString())
                sw.flush()
                sw.close()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun getString(path: String, def: String): String {
        val jStr: String = json().readText()
        val jr = JsonReader()
        if (jStr.isNotEmpty()) {
            for (value in jr.parse(jStr)) {
                if (value.name() == path) {
                    return value.asString()
                }
            }
        }
        return def
    }

    fun getStringList(path: String, def: List<String>): List<String> {
        val jStr: String = json().readText()
        val jr = JsonReader()
        if (jStr.isNotEmpty()) {
            for (value in jr.parse(jStr)) {
                if (value.name() == path) {
                    return ArrayList(listOf(*value.asStringArray()))
                }
            }
        }
        return def
    }

    fun getContent(): String {
        return json().readText()
    }
}

command("translate", "翻译插件") {
    usage = "[info]"
    permission = "cong.translate"
    aliases = listOf("tr")
    body {
        val args = arg
        if (args.isNotEmpty()) {
            when (args[0]) {
                "on" -> {
                    enable = true
                    reply("[sky]全局翻译已开启".with())
                }

                "off" -> {
                    enable = false
                    reply("[sky]全局翻译已关闭".with())
                }

                "add" -> {
                    if (args.size < 3) {
                        reply("[sky]参数不足".with())
                    }
                    if (args[1].equals(args[2], ignoreCase = true)) {
                        reply("[sky]你他妈的想干什么".with())
                    }
                    val list =
                        ConfigUtils.getStringList("list", ArrayList()) as MutableList<String>
                    val target = args[1] + "-" + args[2]
                    if (list.contains(target)) {
                        reply("[sky]有了".with())
                    }
                    list.add(target)
                    ConfigUtils["list"] = list
                    reply("[sky]添加成功:${args[1]}-${args[2]}".with())
                }

                "remove" -> {
                    if (args.size < 3) {
                        reply("[sky]参数不足".with())
                    }
                    val list: List<String> = ConfigUtils.getStringList("list",
                        ArrayList()
                    )
                    val nList: MutableList<String> = ArrayList()
                    list.forEach(Consumer { target: String ->
                        if (!(target.startsWith(args[1]) && target.endsWith(args[2]))) {
                            nList.add(target)
                        }
                    })
                    ConfigUtils["list"] = nList
                    reply("[sky]删除成功:${args[1]}-${args[2]}".with())
                }

                "removeAll" -> {
                    ConfigUtils["list"] = ArrayList<Any>()
                    reply("[sky]清空成功".with())
                }

                "list" -> {
                    val list: List<String> = ConfigUtils.getStringList("list",
                        ArrayList()
                    )
                    reply("[sky]目前的筛选列表:${list.toTypedArray().contentToString()}".with())
                }

                "rule" -> {
                    if (args.size < 3) {
                        reply("[sky]参数不足".with())
                        reply("[sky]当前可设置规则：auto(自动识别源语言（中文除外）并翻译成中文）".with())
                        reply("[sky]auto(自动识别源语言（中文除外）并翻译成中文）".with())
                        reply("[sky]action(是否使用术语库干涉结果）".with())
                    }
                    when(args[1]){
                        "auto" -> {
                            ConfigUtils["rule_" + args[1]] = args[2].toBoolean()
                            reply("[sky]规则 auto 已被设置为:${args[2]}".with())
                        }
                        "action" -> {
                            ConfigUtils["rule_" + args[1]] = args[2].toBoolean()
                            reply("[sky]规则 action 已被设置为:${args[2]}".with())
                        }
                        else -> reply("未知规则:${args[1]}".with())
                    }
                }

                "content" -> {
                    reply(ConfigUtils.getContent().with())
                }

                "info" -> {
                    reply("[sky]===ServerTranslate(TR) v1.2.0==by:cong====".with())
                    reply("[sky]v1.0.0:由lliiooll创建项目 2022.8.10".with())
                    reply("[sky]v1.2.0:由cong做了些许优化 2022.8.12".with())
                    reply("[sky]v1.3.0:由cong移植到sa上 2022.11.27".with())
                }

                else -> reply("未知指令:${args[0]}".with())
            }
        } else {
            reply("[sky]===TR v1.2.0 指令帮助====".with())
            reply("[sky]/tr info 查看版本及更新日志".with())
            reply("[sky]/tr on/off 开关全局翻译".with())
            reply("[sky]/tr add/remove [源语言] [目标语言] 添加源语言(zh)到目标语言(en)的翻译筛选".with())
            reply("[sky]/tr removeAll 移除所有筛选".with())
            reply("[sky]/tr list 查看当前筛选列表".with())
            reply("[sky]/tr rule [参数] [ture/false]设置规则".with())
            reply("[sky]当前可设置规则：auto(自动识别源语言（中文除外）并翻译成中文）".with())
            reply("[sky]auto(自动识别源语言（中文除外）并翻译成中文）".with())
            reply("[sky]action(是否使用术语库干涉结果）".with())
        }
    }
}

listen<EventType.ServerLoadEvent> {
    instance = this
    Log.info("Registered command")
}

listen<EventType.PlayerChatEvent> {
    if (it.message.startsWith("/")) {
        return@listen
    }
    if (it.message.startsWith("<")) {
        return@listen
    }
    if (enable) {
        val auto: Boolean = ConfigUtils.getString("rule_auto", "false") == "true"
        val action: Boolean = ConfigUtils.getString("rule_action", "false") == "true"
        if (auto && !it.player.locale.equals("zh", ignoreCase = true)) {
            broadcast("[sky][TR][yellow][${it.player.name}]:${baiduFanyi.translate(it.message, "zh", action)}".with())
        } else {
            val list: List<String> =
                ConfigUtils.getStringList("list", ArrayList())
            if (list.isEmpty()) {
                return@listen
            }
            list.forEach(Consumer { target: String ->
                if (true) {
                    val to =
                        target.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    broadcast("[sky][TR][yellow][${it.player.name}]:${baiduFanyi.translate(it.message, to, action)}".with())
                    broadcast("[sky][TR][yellow][${it.player.name}]:${baiduFanyi.Website(it.message, to, action)}".with())
                }
            })
        }
    }
}