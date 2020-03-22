import cf.wayzer.script_agent.MindustryMainImpl

command("sMod","ScriptAgent模块控制指令","<reload/list> [name]"){arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    when(arg[0]){
        "reload" -> {
            val list = MindustryMainImpl.manager.loadedInitScripts
            val initS = list.singleOrNull()?:list.firstOrNull{ it::class.java.simpleName == arg.getOrNull(1) }
                ?: return@command player.sendMessage("[red]错误的模块名")
            if(MindustryMainImpl.manager.reloadInit(initS)!=null)
                player.sendMessage("[green]重载成功")
            else
                player.sendMessage("[red]加载失败")
        }
        "list" -> {
            val list = MindustryMainImpl.manager.loadedInitScripts
            player.sendMessage("""
                |[yellow]====已加载模块====
                |${list.joinToString("\n") { "[red]%-20s [green]:%s".format(it::class.java.simpleName, with(it) { name.get() }) }}
            """.trimMargin())
        }
        else -> return@command player.sendMessage("[red]请输入正确的操作")
    }
}

command("sReload","重载ScriptAgent一个脚本","<name> [modName]"){arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val list = MindustryMainImpl.manager.loadedInitScripts
    val initS = list.singleOrNull()?:list.firstOrNull{ it::class.java.simpleName == arg.getOrNull(1) }
        ?: return@command player.sendMessage("[red]找不到模块")
    val contentS = with(initS){children.get().firstOrNull { it::class.java.simpleName == arg[0] }}
        ?: return@command player.sendMessage("[red]找不到脚本")
    if(MindustryMainImpl.manager.reloadContent(initS,contentS)!=null)
        player.sendMessage("[green]重载成功")
    else
        player.sendMessage("[red]加载失败")
}

command("sList","列出ScriptAgent某一模块的所有脚本","[modName]") { arg, player ->
    if (player?.isAdmin == false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val list = MindustryMainImpl.manager.loadedInitScripts
    val initS = list.singleOrNull() ?: list.firstOrNull { it::class.java.simpleName == arg.getOrNull(0) }
        ?: return@command player.sendMessage("[red]找不到模块")
    val children = with(initS){children.get()}
    player.sendMessage("""
                |[yellow]====已加载脚本====
                |${children.joinToString("\n") { it::class.java.simpleName}}
            """.trimMargin())
}
