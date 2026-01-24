package bootStrap

suspend fun boot() = ScriptManager.transaction {
    //compiler plugin
    add("coreLibrary/kcp")
    load();enable()

    //add 添加需要加载的脚本(前缀判断) exclude 排除脚本(可以作为依赖被加载)
    addAll()
    exclude("@")
    exclude("bootStrap/")
    exclude("coreLibrary/extApi/")//lazy load
    exclude("scratch")
    exclude("mirai")//Deprecated
    load()

    exclude("mapScript/")
    enable()
}

onEnable {
    if (Config.mainScript != id)
        return@onEnable ScriptManager.disableScript(this, "仅可通过SAMain启用")
    boot()
}