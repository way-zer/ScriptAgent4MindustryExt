@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.events.ScriptDisableEvent
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.event.PermissionRequestEvent
import coreLibrary.lib.util.Provider
import java.util.logging.Logger


class CommandContext : DSLBuilder() {
    // Should init in CommandInfo
    lateinit var thisCommand: CommandInfo

    // Should init if not empty
    var prefix: String = ""

    // Should init if not empty
    var arg = emptyList<String>()

    // Should init if need
    var reply: (msg: PlaceHoldString) -> Unit = {}

    // Should not null if do TabComplete
    var replyTabComplete: ((list: List<String>) -> Nothing)? = null
    var hasPermission: (node: String) -> Boolean = {
        PermissionRequestEvent(it, this).run {
            emit()
            result == true
        }
    }

    fun new(body: CommandContext.() -> Unit): CommandContext {
        return CommandContext().also {
            it.thisCommand = thisCommand
            it.prefix = prefix
            it.arg = arg
            it.reply = reply
            it.replyTabComplete = replyTabComplete
            it.hasPermission = hasPermission
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

    fun onComplete(index: Int,body:()->List<String>){
        if(replyTabComplete!=null&&arg.size==index+1)
            replyTabComplete?.invoke(body())
    }

    fun endComplete(){
        if(replyTabComplete!=null)
            CommandInfo.Return()
    }

    //return null if empty or no match
    fun <T> checkArg(index: Int, list: List<T>, map: (T) -> String): T? {
        onComplete(index){list.map(map)}
        return arg.getOrNull(index)?.let { a->
            list.find { map(it).equals(a,true) }
        }
    }
    //return null if empty or no match
    fun <T> checkArg(index: Int, map: Map<String,T>,keyHandler:(String)->String={it}): T? {
        onComplete(index){map.keys.toList()}
        return arg.getOrNull(index)?.let { map[keyHandler(it)]}
    }

    fun <T> checkArg(index: Int,map:(String?)->T):T?{
        try {
            return map(arg.getOrNull(index))
        }catch (e : Exception){
            reply("[red]{msg}".with("msg" to (e.message?:"参数错误")))
            throw CommandInfo.Return
        }
    }
}
typealias CommandHandler = CommandContext.() -> Unit

class CommandInfo(val script: IContentScript?, val name: String, val description: String, init: CommandInfo.() -> Unit = {}, private val handler: CommandHandler) : DSLBuilder(), (CommandContext) -> Unit {
    var usage = ""
    var aliases = emptyList<String>()
    var permission = ""
    var supportCompletion = false

    init {
        init()
    }

    override fun invoke(context: CommandContext) {
        context.thisCommand = this
        if(handler !is Commands && !supportCompletion)
            context.endComplete()
        if (permission.isNotBlank() && !context.hasPermission(permission))
            return context.replyNoPermission()
        try {
            handler.invoke(context)
        } catch (e: Return) {
        } catch (e: Exception) {
            context.reply("[red]执行命令出现异常: {msg}".with("msg" to (e.message ?: "")))
            e.printStackTrace()
        }
    }
    object Return : Throwable("Direct return command"){
        operator fun invoke():Nothing{
            throw this
        }
    }
}

open class Commands : (CommandContext) -> Unit {
    protected val subCommands = mutableMapOf<String, CommandInfo>()
    open fun getSub(context: CommandContext):CommandHandler?{
        return context.checkArg(0,subCommands) { it.toLowerCase() }
    }
    override operator fun invoke(context: CommandContext) {
        getSub(context)?.invoke(context.new {
            if(arg.isEmpty())return@new
            prefix += arg[0]+" "
            arg = arg.subList(1, arg.size)
        })?:onHelp(context,false)
    }

    open fun onHelp(context: CommandContext,explicit:Boolean){
        val showDetail = context.arg.lastOrNull() == "-v"
        val list = subCommands.values.toSet().map {
            val alias = if (it.aliases.isEmpty()) "" else it.aliases.joinToString(prefix = "(", postfix = ")")
            val detail = buildString {
                if (!showDetail) return@buildString
                if (it.script != null) append(" | ${it.script.id}")
                if (it.permission.isNotBlank()) append(" | ${it.permission}")
            }
            "[light_yellow]{prefix}{name}[light_red]{aliases} [white]{usage}  [light_cyan]{desc}[cyan]{detail}\n".with(
                    "prefix" to context.prefix, "name" to it.name, "aliases" to alias,
                    "usage" to it.usage, "desc" to it.description, "detail" to detail)
        }
        context.reply("""
                [green]==== [light_yellow]{name}[green] ====
                {list}
            """.trimIndent().with("list" to list, "name" to context.prefix)
        )
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

    open fun addSub(command: CommandInfo) {
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

    operator fun plusAssign(command: CommandInfo) = addSub(command)
    fun autoRemove(script: IContentScript) {
        script.onDisable {
            removeAll(script)
        }
    }
    init {
        subCommands["help"] = CommandInfo(null,"help","帮助指令",{usage="[page] [-v]"}){
            prefix = prefix.removeSuffix("help ")
            onHelp(this,true)
        }
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
            Commands::class.java.getContextModule()!!.listenTo<ScriptDisableEvent> {
                rootProvider.get()?.removeAll(script)
            }
        }
    }
}