package coreLibrary

val thisRef = this
onEnable {
    ICommands.controlCommand.run {
        addSub(ICommand(thisRef, "config", "查看或修改配置", "<path/reload> [set <value>/reset]") { arg ->
            if (!hasPermission("scriptAgent.config")) return@ICommand sendMessage("[red]你没有权限使用该命令".with())
            if (arg.isEmpty()) return@ICommand sendMessage("[red]请输入配置项路径(reload重载配置文件)".with())
            if (arg[0].equals("reload", true)) {
                ConfigBuilder.reloadFile()
                return@ICommand sendMessage("[green]重载成功".with())
            }
            val config = ConfigBuilder.all[arg[0]] ?: return@ICommand sendMessage("[red]找不到配置项".with())
            when {
                arg.size == 1 -> {
                    sendMessage("""
                        [yellow]==== [light_yellow]配置项: {name}[yellow] ====
                        {desc}
                        [cyan]当前值: [yellow]{value}
                    """.trimIndent().with("name" to config.path, "desc" to config.desc.map { "[purple]$it\n" },
                            "value" to config.getString()))
                }
                arg[1].equals("reset", true) -> {
                    fun <T : Any> ConfigBuilder.ConfigKey<T>.reset() {
                        set(default)
                    }
                    config.reset()
                    sendMessage("[green]重置成功,当前:[yellow]{value}".with("value" to config.getString()))
                }
                arg[1].equals("set", true) -> {
                    if (arg.size <= 2) return@ICommand sendMessage("[red]请输入值".with())
                    val value = arg.subList(2, arg.size).joinToString(" ")
                    sendMessage("[green]设置成功,当前:[yellow]{value}".with("value" to config.setString(value)))
                }
            }
        })
        onDisable { removeAll(thisRef) }
    }
}