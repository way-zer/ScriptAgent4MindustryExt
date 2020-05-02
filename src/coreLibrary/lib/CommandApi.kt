package coreLibrary.lib

import cf.wayzer.script_agent.IBaseScript
import cf.wayzer.script_agent.IContentScript
import coreLibrary.lib.commands.IControlCommands
import coreLibrary.lib.util.Provider
import java.util.logging.Logger

/**
 * CommandApi
 * [ICommand] stands for a command
 * [ICommands] use for merge commands and provide help command
 * Support nested
 *
 * Platform should implement [ISender]
 * and recommend make type aliases for [ICommand] and [ICommands]
 */

interface ISender<G> {
    fun sendMessage(msg: PlaceHoldString)
    fun hasPermission(node: String): Boolean
    val player: G
}

open class ICommand<S : ISender<*>>(
        val script: IBaseScript?,
        val name: String,
        val description: String,
        val usage: String = "",
        val aliases: List<String> = emptyList(),
        private val handle: S.(arg: List<String>) -> Unit
) {
    open fun handle(sender: S, arg: List<String>, prefix: String) {
        handle.invoke(sender, arg)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ICommand<*>

        if (script != other.script) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = script?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "Command(script=${script?.clsName}, name='$name', description='$description')"
    }
}

/**
 * For rootCommand,can overwrite [addSub] and [removeSub]
 */
@Suppress("MemberVisibilityCanBePrivate")
open class ICommands<S : ISender<*>>(script: IBaseScript?, name: String, description: String, aliases: List<String> = emptyList()) : ICommand<S>(
        script, name, description, "[help]", aliases, {}
) {
    protected val subCommands = mutableMapOf<String, ICommand<in S>>()
    override fun handle(sender: S, arg: List<String>, prefix: String) {
        val cmd: ICommand<in S> = subCommands[arg.getOrNull(0)?.toLowerCase()] ?: subCommands["help"]!!
        cmd.handle(sender, if (arg.isNotEmpty()) arg.subList(1, arg.size) else emptyList(), prefix + " " + cmd.name)
    }

    protected open fun addSub(name: String, command: ICommand<in S>, isAliases: Boolean) {
        val existed = subCommands[name.toLowerCase()] ?: let {
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

    fun addSub(command: ICommand<in S>) {
        addSub(command.name, command, false)
        command.aliases.forEach {
            addSub(it, command, true)
        }
    }

    protected open fun removeSub(name: String) {
        subCommands.remove(name)
    }

    open fun removeAll(script: IContentScript) {
        val toRemove = mutableListOf<String>()
        subCommands.forEach { (k, s) ->
            if (s.script == script) toRemove.add(k)
        }
        toRemove.forEach(::removeSub)
    }

    init {
        this.addSub(HelpCommand())
    }

    inner class HelpCommand : ICommand<ISender<*>>(null, "help", "帮助", handle = {}) {
        override fun handle(sender: ISender<*>, arg: List<String>, prefix: String) {//Need to use prefix
            val list = subCommands.values.toSet().map {
                "[purple]{prefix} {name}[blue]({aliases}) [purple]{usage} [light_purple]{desc} [purple]FROM [light_purple]{script}\n".with(
                        "prefix" to prefix.removeSuffix(" help"), "name" to it.name, "aliases" to it.aliases.joinToString(),
                        "usage" to it.usage, "desc" to it.description, "script" to (it.script?.clsName ?: "UNKNOWN")
                )
            }
            sender.sendMessage(
                    """
                [yellow]==== [light_yellow]{name}[yellow] ====
                {list}
            """.trimIndent().with("list" to list, "name" to name)
            )
        }
    }

    companion object {
        val rootProvider = Provider<ICommands<out ISender<*>>>()
        val controlCommand = IControlCommands()

        init {
            rootProvider.every {
                it.addSub(controlCommand)
            }
        }
    }
}