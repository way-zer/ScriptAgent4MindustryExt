package coreLibrary.commands

import cf.wayzer.placehold.PlaceHoldApi.with

val configCommands = Commands()
command("config", "查看或修改配置".with(), commands = Commands.controlCommand) {
    usage = "[help/arg...]"
    requirePermission("scriptAgent.$name")
    onComplete {
        try {
            configCommands.handle()
        } catch (_: CommandInfo.Return) {
        }
        onCompleteArg(0) {
            remove("<key>")
            configCommands.getSub("get")?.handle()
        }
    }
    body {
        if (arg.firstOrNull() in ConfigBuilder.all) {
            configCommands.getSub("<key>")?.handle()
            return@body
        }
        configCommands.handle()
    }
}

command("<key>", "快速get/set配置项".with(), commands = configCommands) {
    usage = "[value]"
    body {
        when (arg.size) {
            0 -> returnReply("[red]该命令不能直接调用".with())
            1 -> configCommands.getSub("get")?.handle()
            else -> configCommands.getSub("set")?.handle()
        }
    }
}
command("list", "列出所有配置项".with(), commands = configCommands) {
    usage = "[page]"
    body {
        val page = arg.getOrNull(0)?.toIntOrNull() ?: 1
        reply(menu("配置项", ConfigBuilder.all.values.sortedBy { it.path }, page, 15) {
            "[green]{key} [blue]{desc}".with(
                "key" to it.path,
                "desc" to (it.desc.firstOrNull() ?: "")
            )
        })
    }
}
command("reload", "重载配置文件".with(), commands = configCommands) {
    requirePermission("scriptAgent.config.$name")
    body {
        ConfigBuilder.reloadFile()
        reply("[green]重载成功".with())
    }
}

@CommandInfo.CommandBuilder
inline fun CommandInfo.subCommand(
    usage: String,
    crossinline block: suspend context(CommandContext) (ConfigBuilder.ConfigKey<*>) -> Unit
) {
    this.usage = "<key> $usage"
    onComplete {
        onComplete(0) { ConfigBuilder.all.keys.toList() }
    }
    body {
        val config = arg.firstOrNull()?.let { ConfigBuilder.all[it] } ?: returnReply("[red]找不到配置项".with())
        if (!hasPermission("scriptAgent.config." + config.path))
            returnReply("[red]你没有权限修改配置项: {config}".with("config" to config.path))
        block(context, config)
    }
}
command("get", "获取配置项".with(), commands = configCommands) {
    subCommand("") { config ->
        reply(
            """
                        |[yellow]==== [light_yellow]配置项: {name}[yellow] ====
                        |[purple]{desc|joinLines}
                        |[cyan]当前值: [yellow]{value}
                        |[cyan]默认值: [yellow]{default}
                        |[yellow]使用/sa config help查看可用操作
                    """.trimMargin().with(
                "name" to config.path, "desc" to config.desc,
                "value" to config.getString(), "default" to config.default,
            )
        )
    }
}
command("set", "设置配置项".with(), commands = configCommands) {
    subCommand("<value>") { config ->
        if (arg.size <= 1) returnReply("[red]请输入值".with())
        val value = arg.subList(1, arg.size).joinToString(" ")
        reply("[green]设置成功,当前:[yellow]{value}".with("value" to config.setString(value)))
    }
}
command("reset", "恢复默认值".with(), commands = configCommands) {
    subCommand("") { config ->
        config.reset()
        reply("[green]恢复成功,当前:[yellow]{value}".with("value" to config.getString()))
    }
}
command("write", "写入配置项到配置文件".with(), commands = configCommands) {
    subCommand("") { config ->
        if (config.get() != config.default)
            config.writeDefault()
        reply("[green]写入文件成功".with())
    }
}