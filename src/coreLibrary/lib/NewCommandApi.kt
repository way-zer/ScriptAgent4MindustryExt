@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.util.DSLBuilder
import java.util.logging.Logger


class CommandContext : DSLBuilder() {
    lateinit var reply: (msg: PlaceHoldString)->Unit
    lateinit var hasPermission:(node: String)->Boolean
    lateinit var thisCommand: CommandInfo
    var prefix: String = ""
    var arg = emptyList<String>()
    fun new(body:CommandContext.()->Unit):CommandContext{
        return CommandContext().also {
            it.data.putAll(data)
            body(it)
        }
    }
}
typealias CommandHandler = CommandContext.() -> Unit

class CommandInfo(val script: IContentScript?, val name:String, val description: String, private val handler:CommandHandler):DSLBuilder(), (CommandContext) -> Unit {
    var usage = ""
    val aliases = emptyList<String>()
    override fun invoke(context: CommandContext) {
        handler.invoke(context)
    }
}
open class NewCommands : (CommandContext) -> Unit {
    protected val subCommands = mutableMapOf<String, CommandInfo>()
    override operator fun invoke(context: CommandContext) {
        if (context.arg.isEmpty()){
            return (subCommands[""]?:subCommands["help"]!!).invoke(context)
        }
        val cmd= subCommands[context.arg[0].toLowerCase()]
        if (cmd == null)subCommands["help"]!!.invoke(context)
        else cmd.invoke(context.new {
            context.prefix += " "+context.arg[0]
            context.arg = arg.subList(1,arg.size)
        })
    }

    protected open fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        val existed = subCommands[name.toLowerCase()]?.takeIf { it.script?.cancelled !=true } ?: let {
            subCommands[name.toLowerCase()] = command
            return
        }
        if (isAliases) {
            Logger.getLogger("[CommandApi]").warning("duplicate aliases $name($command) with $existed")
        } else {
            Logger.getLogger("[CommandApi]").warning("replace command $name: NOW:$command OLD:$existed")
            subCommands[name.toLowerCase()] = command //name is more important
        }
    }

    open fun removeSub(name: String) {
        subCommands.remove(name)
    }

    fun addSub(command: CommandInfo) {
        addSub(command.name, command, false)
        command.aliases.forEach {
            addSub(it, command, true)
        }
    }

    open fun removeAll(script: IContentScript) {
        val toRemove = mutableListOf<String>()
        subCommands.forEach { (k, s) ->
            if (s.script == script) toRemove.add(k)
        }
        toRemove.forEach(::removeSub)
    }

    init {
        this.addSub(CommandInfo(null,"help","显示帮助"){
            val list = subCommands.values.toSet().map {
                "[purple]{prefix} {name}[blue]{aliases} [purple]{usage} [light_purple]{desc} [purple]FROM [light_purple]{script}\n".with(
                        "prefix" to prefix, "name" to it.name, "aliases" to it.aliases.joinToString(prefix="(",postfix = ")"),
                        "usage" to it.usage, "desc" to it.description, "script" to (it.script?.clsName ?: "UNKNOWN")
                )
            }
            reply("""
                [yellow]==== [light_yellow]{name}[yellow] ====
                {list}
            """.trimIndent().with("list" to list, "name" to thisCommand.name)
            )
        })
    }
}