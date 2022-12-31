package main

suspend fun boot() = ScriptManager.transaction {
    //add 添加需要加载的脚本(前缀判断) exclude 排除脚本(可以作为依赖被加载)
    addAll()
//    add("core")
    exclude("coreLibrary/redisApi")
//    add("main")
    exclude("main/scratch")
//    add("wayzer")
    exclude("mirai")


    load()


    exclude("mapScript/")
    enable()
}

onEnable {
    if (Config.mainScript != id)
        return@onEnable ScriptManager.disableScript(this, "仅可通过SAMain启用")
    boot()
}