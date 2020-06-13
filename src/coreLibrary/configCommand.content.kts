package coreLibrary

val thisRef = this
onEnable {
    ICommands.controlCommand.run {
        addSub(ICommand(thisRef, "config", "查看或修改配置", "[help/arg]") { arg ->
            if (!hasPermission("scriptAgent.config")) return@ICommand sendMessage("[red]你没有权限使用该命令".with())
            if (arg.isEmpty() || arg[0].equals("help", true)) return@ICommand sendMessage("""
                        [yellow]可用操作
                        [purple]config reload [light_purple]重载配置文件
                        [purple]config <配置项> set <value> [light_purple]设置配置值
                        [purple]config <配置项> write [light_purple]写入默认值到配置文件
                        [purple]config <配置项> reset [light_purple]恢复默认值（从配置文件移除默认值）
                    """.trimIndent().with())
            if (arg[0].equals("reload", true)) {
                ConfigBuilder.reloadFile()
                return@ICommand sendMessage("[green]重载成功".with())
            }
            val config = ConfigBuilder.all[arg[0]] ?: return@ICommand sendMessage("[red]找不到配置项".with())
            when (arg.getOrNull(1)?.toLowerCase()) {
                null -> {
                    sendMessage("""
                        [yellow]==== [light_yellow]配置项: {name}[yellow] ====
                        {desc}
                        [cyan]当前值: [yellow]{value}
                        [yellow]使用/sa config help查看可用操作
                    """.trimIndent().with("name" to config.path, "desc" to config.desc.map { "[purple]$it\n" },
                            "value" to config.getString()))
                }
                "reset" -> {
                    config.reset()
                    sendMessage("[green]重置成功,当前:[yellow]{value}".with("value" to config.getString()))
                }
                "write" -> {
                    config.writeDefault()
                    sendMessage("[green]写入文件成功成功".with())
                }
                "set" -> {
                    if (arg.size <= 2) return@ICommand sendMessage("[red]请输入值".with())
                    val value = arg.subList(2, arg.size).joinToString(" ")
                    sendMessage("[green]设置成功,当前:[yellow]{value}".with("value" to config.setString(value)))
                }
                else -> {
                    sendMessage("[red]未知操作，请查阅help帮助".with())
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}