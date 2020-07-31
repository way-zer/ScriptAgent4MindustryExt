package coreLibrary

import cf.wayzer.placehold.PlaceHoldApi.with

val thisRef = this
onEnable {
    Commands.controlCommand.run {
        addSub(CommandInfo(thisRef, "config", "查看或修改配置", {
            usage="[help/arg...]"
            permission = "scriptAgent.config"
            supportCompletion = true
        }) {
            onComplete(0){ listOf("help","reload")+ConfigBuilder.all.keys }
            onComplete(1){ listOf("set","write","reset") }
            endComplete()
            if (arg.isEmpty() || arg[0].equals("help", true)) return@CommandInfo reply("""
                        [yellow]可用操作
                        [purple]config reload [light_purple]重载配置文件
                        [purple]config <配置项> [light_purple]查看配置项介绍及当前值
                        [purple]config <配置项> set <value> [light_purple]设置配置值
                        [purple]config <配置项> write [light_purple]写入默认值到配置文件
                        [purple]config <配置项> reset [light_purple]恢复默认值（从配置文件移除默认值）
                    """.trimIndent().with())
            if (arg[0].equals("reload", true)) {
                ConfigBuilder.reloadFile()
                return@CommandInfo reply("[green]重载成功".with())
            }
            val config = checkArg(0,ConfigBuilder.all) ?: return@CommandInfo reply("[red]找不到配置项".with())
            when (arg.getOrNull(1)?.toLowerCase()) {
                null -> {
                    reply("""
                        [yellow]==== [light_yellow]配置项: {name}[yellow] ====
                        {desc}
                        [cyan]当前值: [yellow]{value}
                        [yellow]使用/sa config help查看可用操作
                    """.trimIndent().with("name" to config.path, "desc" to config.desc.map { "[purple]$it\n" },
                            "value" to config.getString()))
                }
                "reset" -> {
                    config.reset()
                    reply("[green]重置成功,当前:[yellow]{value}".with("value" to config.getString()))
                }
                "write" -> {
                    config.writeDefault()
                    reply("[green]写入文件成功成功".with())
                }
                "set" -> {
                    if (arg.size <= 2) return@CommandInfo reply("[red]请输入值".with())
                    val value = arg.subList(2, arg.size).joinToString(" ")
                    reply("[green]设置成功,当前:[yellow]{value}".with("value" to config.setString(value)))
                }
                else -> {
                    reply("[red]未知操作，请查阅help帮助".with())
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}