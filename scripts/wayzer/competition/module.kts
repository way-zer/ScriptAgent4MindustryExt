@file:Import("wayzer.competition.ext.*", defaultImport = true)

package wayzer.competition

/**
 * @author by nanmenyangde
 */
name = "Competition Plugin"

val competition by config.key(false, "是否启用pvp系统")
val solo by config.key(false, "是否启用solo系统")

onEnable{
    if (!competition) {
        ScriptManager.unloadScript(thisContextScript(), "pvp系统未启用")
    }
}