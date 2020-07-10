@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.event.PermissionRequestEvent
import coreLibrary.lib.util.Provider
import java.util.logging.Logger


class CommandContext : DSLBuilder() {
    lateinit var reply: (msg: PlaceHoldString) -> Unit
    var hasPermission: (node: String) -> Boolean = {
        PermissionRequestEvent(it, this).run {
            emit()
            result == true
        }
    }
    lateinit var thisCommand: CommandInfo
    var prefix: String = ""
    var arg = emptyList<String>()
    fun new(body: CommandContext.() -> Unit): CommandContext {
        return CommandContext().also {
            it.reply = reply
            it.hasPermission = hasPermission
            it.thisCommand = thisCommand
            it.prefix = prefix
            it.arg = arg
            it.data.putAll(data)
            body(it)
        }
    }

    fun replyNoPermission() {
        reply("[red]你没有执行该命令的权限".with())
    }

    fun replyUsage() {
        reply("[red]参数错误: {prefix} {usage}".with("prefix" to prefix, "usage" to thisCommand.usage))
    }
}
typealias CommandHandler = CommandContext.() -> Unit

class CommandInfo(val script: IContentScript?, val name: String, val description: String, init: CommandInfo.() -> Unit = {}, private val handler: CommandHandler) : DSLBuilder(), (CommandContext) -> Unit {
    var usage = ""
    var aliases = emptyList<String>()
    var permission = ""

    init {
        init()
    }

    override fun invoke(context: CommandContext) {
        if (permission.isNotBlank() && !context.hasPermission(permission))
            return context.replyNoPermission()
        try {
            handler.invoke(context)
        } catch (e: Exception) {
            context.reply("[red]执行命令出现异常: {msg}".with("msg" to (e.message ?: "")))
            e.printStackTrace()
        }
    }
}

open class Commands : (CommandContext) -> Unit {
    protected val subCommands = mutableMapOf<String, CommandInfo>()
    override operator fun invoke(context: CommandContext) {
        if (context.arg.isEmpty()) {
            return (subCommands[""] ?: subCommands["help"]!!).invoke(context)
        }
        val cmd = subCommands[context.arg[0].toLowerCase()]
        if (cmd == null) subCommands["help"]!!.invoke(context)
        else cmd.invoke(context.new {
            prefix += " " + arg[0]
            arg = arg.subList(1, arg.size)
        })
    }

    protected open fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        val existed = subCommands[name.toLowerCase()]?.takeIf { it.script?.cancelled != true } ?: let {
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
        this.addSub(CommandInfo(null, "help", "显示帮助") {
            val showDetail = arg.getOrNull(0) == "-v"
            val list = subCommands.values.toSet().map {
                val alias = if(it.aliases.isEmpty())"" else it.aliases.joinToString(prefix = "(", postfix = ")")
                val detail = buildString {
                    if (!showDetail) return@buildString
                    if (it.script != null) append("FROM ${it.script.id}")
                    if (it.permission.isNotBlank()) append("REQUIRE ${it.permission}")
                }
                "[purple]{prefix} {name}[blue]{aliases} [purple]{usage} [light_purple]{desc} [purple]{detail}\n".with(
                        "prefix" to prefix, "name" to it.name, "aliases" to alias,
                        "usage" to it.usage, "desc" to it.description, "detail" to detail)
            }
            reply("""
                [yellow]==== [light_yellow]{name}[yellow] ====
                {list}
            """.trimIndent().with("list" to list, "name" to thisCommand.name)
            )
        })
    }

    companion object {
        val rootProvider = Provider<Commands>()
        val controlCommand = Commands()

        init {

            rootProvider.every {
                it.addSub(CommandInfo(null, "ScriptAgent", "ScriptAgent 控制指令", {
                    aliases = listOf("sa")
                    permission = "scriptAgent.admin"
                }, controlCommand))
            }
        }
    }
}