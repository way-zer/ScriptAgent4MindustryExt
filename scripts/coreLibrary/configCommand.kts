package coreLibrary

import cf.wayzer.placehold.PlaceHoldApi.with
import coreLibrary.lib.util.menu

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "config", "查看或修改配置") {
            usage = "[help/arg...]"
            permission = "scriptAgent.config"
            onComplete {
                onComplete(0) { listOf("help", "reload") + ConfigBuilder.all.keys }
                onComplete(1) { listOf("set", "write", "reset") }
            }
            body {
                if (arg.isEmpty() || arg[0].equals("help", true))
                    returnReply(
                        """
                        [yellow]可用操作
                        [purple]config reload [light_purple]重载配置文件
                        [purple]config list [页码] [light_purple]列出配置项
                        [purple]config <配置项> [light_purple]查看配置项介绍及当前值
                        [purple]config <配置项> set <value> [light_purple]设置配置值
                        [purple]config <配置项> write [light_purple]写入默认值到配置文件
                        [purple]config <配置项> reset [light_purple]恢复默认值（从配置文件移除默认值）
                    """.trimIndent().with()
                    )
                if (arg[0].equals("list", true)) {
                    val page = arg.getOrNull(1)?.toIntOrNull() ?: 1
                    returnReply(menu("配置项", ConfigBuilder.all.values.sortedBy { it.path }, page, 15) {
                        "[green]{key} [blue]{desc}".with(
                            "key" to it.path,
                            "desc" to (it.desc.firstOrNull() ?: "")
                        )
                    })
                }
                if (arg[0].equals("reload", true)) {
                    if (!hasPermission("$permission.reload"))
                        replyNoPermission()
                    ConfigBuilder.reloadFile()
                    returnReply("[green]重载成功".with())
                }
                val config = arg.firstOrNull()?.let { ConfigBuilder.all[it] } ?: returnReply("[red]找不到配置项".with())
                if (!hasPermission(permission + "." + config.path))
                    returnReply("[red]你没有权限修改配置项: {config}".with("config" to config.path))
                when (arg.getOrNull(1)?.lowercase()) {
                    null -> {
                        returnReply(
                            """
                        |[yellow]==== [light_yellow]配置项: {name}[yellow] ====
                        |[purple]{desc:${"\n"}}
                        |[cyan]当前值: [yellow]{value}
                        |[yellow]使用/sa config help查看可用操作
                    """.trimMargin().with(
                                "name" to config.path, "desc" to config.desc,
                                "value" to config.getString()
                            )
                        )
                    }
                    "reset" -> {
                        config.reset()
                        returnReply("[green]重置成功,当前:[yellow]{value}".with("value" to config.getString()))
                    }
                    "write" -> {
                        config.writeDefault()
                        reply("[green]写入文件成功".with())
                    }
                    "set" -> {
                        if (arg.size <= 2) returnReply("[red]请输入值".with())
                        val value = arg.subList(2, arg.size).joinToString(" ")
                        returnReply("[green]设置成功,当前:[yellow]{value}".with("value" to config.setString(value)))
                    }
                    else -> {
                        returnReply("[red]未知操作，请查阅help帮助".with())
                    }
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}