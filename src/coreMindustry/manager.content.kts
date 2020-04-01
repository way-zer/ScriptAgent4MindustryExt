//WayZer 版权所有(请勿删除版权注解)
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IInitScript

name="基础: 插件管理"


fun getModuleByName(name:String?): IInitScript? {
    val list = Config.inst.loadedInitScripts
    return list.values.singleOrNull()?:list[name?.toLowerCase()]
}

command("sMod","ScriptAgent模块控制指令","<reload/list> [name]"){arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    when(arg[0]){
        "reload" -> {
            val initS = getModuleByName(arg.getOrNull(1))?: return@command player.sendMessage("[red]错误的模块名")
            if(Config.inst.reloadInit(initS)!=null)
                player.sendMessage("[green]重载成功")
            else
                player.sendMessage("[red]加载失败")
        }
        "list" -> {
            val list = Config.inst.loadedInitScripts.values
            player.sendMessage("""
                |[yellow]====已加载模块====
                |${list.joinToString("\n") { "[red]%-20s [green]:%s".format(it.clsName, it.name) }}
            """.trimMargin())
        }
        else -> return@command player.sendMessage("[red]请输入正确的操作")
    }
}

command("sReload","重载ScriptAgent一个脚本","<name> [modName]"){arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val initS = getModuleByName(arg.getOrNull(1))?: return@command player.sendMessage("[red]找不到模块")
    val contentS = initS.children.firstOrNull { it.clsName.equals(arg[0],true)}
        ?: return@command player.sendMessage("[red]找不到脚本")
    if(Config.inst.reloadContent(initS,contentS)!=null)
        player.sendMessage("[green]重载成功")
    else
        player.sendMessage("[red]加载失败")
}

command("sList","列出ScriptAgent某一模块的所有脚本","[modName]") { arg, player ->
    if (player?.isAdmin == false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val initS = getModuleByName(arg.getOrNull(0))?: return@command player.sendMessage("[red]找不到模块")
    val children = initS.children
    player.sendMessage("""
                |[yellow]====已加载脚本====
                |${children.joinToString("\n") { "[red]%-20s [green]:%s".format(it.clsName, it.name) }}
            """.trimMargin())
}

command("sload", "新加载单个文件", "<filename>") { arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val file = Config.rootDir.resolve(arg[0])
    if (!file.exists() || !file.isFile) player.sendMessage("[red]未找到对应文件")
    val success = if (file.name.endsWith(".init.kts")) {
        Config.inst.loadModule(file) != null
    } else {
        val list = Config.inst.loadedInitScripts.values
        val initS = list.firstOrNull { it.clsName.equals(arg[0].split("/")[0],true)}
                ?: return@command player.sendMessage("[red]找不到模块,请确定模块已先加载")
        Config.inst.loadContent(initS, file) != null
    }
    if (success)
        player.sendMessage("[green]加载成功")
    else
        player.sendMessage("[red]]加载失败")
}