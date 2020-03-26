//WayZer 版权所有(请勿删除版权注解)
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.IInitScript
import cf.wayzer.script_agent.MindustryMainImpl

name.set("基础: 插件管理")

val IInitScript.clsName get() = this::class.java.simpleName.removeSuffix("_init")
val IContentScript.clsName get() = this::class.java.simpleName.removeSuffix("_content")

fun getModuleByName(name:String?): IInitScript? {
    val list = MindustryMainImpl.manager.loadedInitScripts
    return list.singleOrNull()?:list.firstOrNull{ it.clsName.equals(name,true)}
}

command("sMod","ScriptAgent模块控制指令","<reload/list> [name]"){arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    when(arg[0]){
        "reload" -> {
            val initS = getModuleByName(arg.getOrNull(1))?: return@command player.sendMessage("[red]错误的模块名")
            if(MindustryMainImpl.manager.reloadInit(initS)!=null)
                player.sendMessage("[green]重载成功")
            else
                player.sendMessage("[red]加载失败")
        }
        "list" -> {
            val list = MindustryMainImpl.manager.loadedInitScripts
            player.sendMessage("""
                |[yellow]====已加载模块====
                |${list.joinToString("\n") { "[red]%-20s [green]:%s".format(it.clsName, with(it) { name.get() }) }}
            """.trimMargin())
        }
        else -> return@command player.sendMessage("[red]请输入正确的操作")
    }
}

command("sReload","重载ScriptAgent一个脚本","<name> [modName]"){arg, player ->
    if(player?.isAdmin ==false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val initS = getModuleByName(arg.getOrNull(1))?: return@command player.sendMessage("[red]找不到模块")
    val contentS = with(initS){children.get().firstOrNull { it.clsName.equals(arg[0],true)}}
        ?: return@command player.sendMessage("[red]找不到脚本")
    if(MindustryMainImpl.manager.reloadContent(initS,contentS)!=null)
        player.sendMessage("[green]重载成功")
    else
        player.sendMessage("[red]加载失败")
}

command("sList","列出ScriptAgent某一模块的所有脚本","[modName]") { arg, player ->
    if (player?.isAdmin == false)
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val initS = getModuleByName(arg.getOrNull(0))?: return@command player.sendMessage("[red]找不到模块")
    val children = with(initS){children.get()}
    player.sendMessage("""
                |[yellow]====已加载脚本====
                |${children.joinToString("\n") { "[red]%-20s [green]:%s".format(it.clsName, with(it) { name.get() }) }}
            """.trimMargin())
}

command("sload", "新加载单个文件", "<filename>") { arg, p ->
    if(player?.isAdmin ==false)
        return@command p.sendMessage("[red]你没有权限使用该命令")
    val file = MindustryMainImpl.manager.rootDir.resolve(arg[0])
    if (!file.exists() || !file.isFile) p.sendMessage("[red]未找到对应文件")
    val success = if (file.name.endsWith(".init.kts")) {
        MindustryMainImpl.manager.loadModule(file) != null
    } else {
        val list = MindustryMainImpl.manager.loadedInitScripts
        val initS = list.firstOrNull { it.clsName.equals(arg[0].split("/")[0],true)}
                ?: return@command p.sendMessage("[red]找不到模块,请确定模块已先加载")
        MindustryMainImpl.manager.loadContent(initS, file) != null
    }
    if (success)
        p.sendMessage("[green]加载成功")
    else
        p.sendMessage("[red]]加载失败")
}