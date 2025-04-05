@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.events.ScriptDisableEvent
import cf.wayzer.scriptAgent.listenTo
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.PlaceHold.registerForType
import coreLibrary.lib.util.menu
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.cancellation.CancellationException

class CommandContext : DSLBuilder(), Cloneable {
    var receiver: Any = ConsoleReceiver

    // Should init if not empty
    var prefix: String = ""

    // Should init if not empty
    var arg = emptyList<String>()

    /** use for arg like '-v' */
    fun checkArg(p: String): Boolean {
        if (p !in arg) return false
        arg = arg.filterNot { it == p }
        return true
    }

    inline fun <T> resolveArg(name: String, default: T, block: (String) -> T): T {
        if (arg.isEmpty()) return default
        try {
            val value = block(arg.first())
            arg = arg.drop(1)
            return value
        } catch (e: Exception) {
            returnReply("[red]参数解析错误 {name}: {e}".with("name" to name, "e" to e))
        }
    }

    /**
     * message callback
     * should support async, otherwise set to {} after use
     * should support call from other thread, switch thread when need
     */
    var reply: (msg: PlaceHoldString) -> Unit = {}

    // Should not null if do TabComplete
    var replyTabComplete: ((list: List<String>) -> Nothing)? = null

    // Should init in RootCommand
    var hasPermission: suspend (node: String) -> Boolean = { false }

    fun getSub(): CommandContext {
        return (clone() as CommandContext).apply {
            if (arg.isEmpty()) return@apply
            prefix += arg[0] + " "
            arg = arg.subList(1, arg.size)
        }
    }

    //===util===
    /**Can't be call in coroutine or other context, use [reply] instead*/
    @CommandInfo.CommandBuilder
    fun returnReply(msg: PlaceHoldString): Nothing {
        reply(msg)
        CommandInfo.Return()
    }

    inline val context get() = this

    /** receiver for reply */
    object ConsoleReceiver {
        init {
            registerForType<ConsoleReceiver>(thisContextScript()).apply {
                registerChild("colorHandler", "颜色变量处理", DynamicVar.obj {
                    Color::convertToAnsiCode
                })
            }
        }
    }
}

typealias CommandHandlerOld = suspend CommandContext.() -> Unit

fun interface CommandHandler {
    context(CommandContext) suspend fun handle()
}

interface TabCompleter {
    suspend fun onComplete(context: CommandContext)
    fun CommandContext.onComplete(index: Int, body: () -> List<String>) {
        if (arg.size == index + 1)
            replyTabComplete?.invoke(body())
    }
}

class CommandInfo(
    val script: Script?,
    val name: String,
    val description: PlaceHoldString,
    var aliases: List<String> = emptyList(),
) : DSLBuilder(), CommandHandler, TabCompleter {
    constructor(script: Script?, name: String, description: PlaceHoldString, init: CommandInfo.() -> Unit)
            : this(script, name, description) {
        init()
    }

    constructor(script: Script?, name: String, description: String, init: CommandInfo.() -> Unit = {})
            : this(script, name, description.with(), init)
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    constructor(script: Script?, name: String, description: PlaceHoldString) : this(script, name, description)

    val attrs: List<CommandHandler> = mutableListOf()
    var usage: String = ""

    @Deprecated("use RequirePermission(permission)")
    var permission: String = ""
    private var onComplete: CommandHandler = CommandHandler {}
    private var body: CommandHandler = CommandHandler {}
    private var frozen = false

    fun freeze() {
        if (frozen) return
        @Suppress("DEPRECATION")
        if (permission.isNotEmpty())
            attr(RequirePermission(permission))
        frozen = true
    }

    /**
     * Add a attr to this command, will run before body
     */
    @CommandBuilder
    fun attr(beforeBody: CommandHandler) {
        if (frozen) error("This command is already frozen, you must add attr before body")
        (attrs as MutableList).add(beforeBody)
    }

    @Deprecated("replace CommandHandler", level = DeprecationLevel.HIDDEN)
    fun onComplete(block: CommandHandlerOld) = onComplete {
        block.invoke(context)
    }
    @CommandBuilder
    fun onComplete(body: CommandHandler) {
        this.onComplete = body
    }

    @Deprecated("replace CommandHandler", level = DeprecationLevel.HIDDEN)
    fun body(block: CommandHandlerOld) {
        if (block is CommandHandler) return body(block)
        body {
            block.invoke(context)
        }
    }

    @CommandBuilder
    fun body(body: CommandHandler) {
        if (frozen) error("This command is already frozen")
        this.body = body
        freeze()
    }

    override suspend fun onComplete(context: CommandContext) {
        context.run { onComplete.handle() }
        (body as? TabCompleter)?.onComplete(context)
    }

    context(CommandContext) override suspend fun handle() {
        try {
            attrs.forEach { it.handle() }
            body.handle()
        } catch (e: CancellationException) {
            if (e !is Return)
                thisContextScript().logger.log(
                    Level.WARNING, "You should not cancel command. If you need exit, using CommandInfo.Return()", e
                )
        } catch (e: Exception) {
            reply("[red]执行命令出现异常: {msg}".with("msg" to (e.message ?: "")))
            e.printStackTrace()
        }
    }

    @CommandBuilder
    @Deprecated("use +RequirePermission(permission)")
    fun CommandContext.replyNoPermission(): Nothing {
        reply("[red]你没有执行该命令的权限".with())
        Return()
    }

    @CommandBuilder
    fun CommandContext.replyUsage(): Nothing {
        reply("[red]参数错误: {prefix} {usage}".with("prefix" to prefix, "usage" to usage))
        Return()
    }

    override fun toString(): String {
        return "CommandInfo(name='$name', script=$script, description=$description)"
    }

    data object Return : CancellationException("Direct return command") {
        private fun readResolve(): Any = Return
        @CommandBuilder
        operator fun invoke(): Nothing {
            throw this
        }
    }

    @DslMarker
    annotation class CommandBuilder
}

open class Commands : CommandHandler, TabCompleter, CommandHandlerOld {
    fun interface Hidden : CommandHandler {
        /** 当前命令是否可用, 用于[Commands.helpCommand]处理 */
        context(CommandContext) suspend fun visible(): Boolean
        context(CommandContext) override suspend fun handle() {
            if (!visible()) returnReply("[red]该命令当前不可用".with())
        }
    }

    protected val nameMap = mutableMapOf<String, CommandInfo>()
    open fun subCommands(): Map<String, CommandInfo> = nameMap
    fun getSub(name: String): CommandInfo? = subCommands()[name.lowercase()]

    override suspend fun onComplete(context: CommandContext) {
        context.onComplete(0) { subCommands().keys.toList() }
        if (context.arg.size > 1)
            getSub(context.arg.first())?.onComplete(context.getSub())
    }

    context(CommandContext) override suspend fun handle() {
        if (arg.isEmpty()) return helpCommand.handle()
        val name = arg.first()
        with(getSub()) {
            getSub(name)?.handle()?.let { return }
        }
        reply(
            "[red]无效指令\"{name}\",请使用 {prefix}help 查询".with("name" to name, "prefix" to prefix)
        )
    }

    protected open fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        val existed = nameMap[name.lowercase()]?.takeIf { it.script?.enabled == true } ?: let {
            nameMap[name.lowercase()] = command
            return
        }
        if (existed == command) return
        if (isAliases) {
            Logger.getLogger("[CommandApi]").warning("duplicate aliases $name($command) with $existed")
        } else {
            Logger.getLogger("[CommandApi]").warning("replace command $name: NOW:$command OLD:$existed")
            nameMap[name.lowercase()] = command //name is more important
        }
    }

    fun addSub(command: CommandInfo) {
        addSub(command.name, command, false)
        command.aliases.forEach {
            addSub(it, command, true)
        }
    }

    fun removeSub(command: CommandInfo) {
        nameMap.remove(command.name.lowercase(), command)
        command.aliases.forEach {
            nameMap.remove(it.lowercase(), command)
        }
    }

    open fun removeAll(script: Script) {
        val toRemove = mutableListOf<String>()
        nameMap.forEach { (k, s) ->
            if (s.script == script) toRemove.add(k)
        }
        toRemove.forEach {
            nameMap.remove(it.lowercase())
        }
    }

    operator fun plusAssign(command: CommandInfo) = addSub(command)
    @Deprecated(
        "recommend listenTo<ScriptDisableEvent> { removeAll(script) }",
        ReplaceWith("script.onDisable { removeAll(script) }")
    )
    fun autoRemove(script: Script) {
        script.onDisable {
            removeAll(script)
        }
    }

    val helpCommand = CommandInfo(null, "help", "帮助指令".with()).apply {
        usage = "[-v] [page=1]"
        aliases = listOf("帮助")
        body {
            val showAll = checkArg("-v")
            val page = resolveArg("page", 1) { it.toInt() }
            prefix = prefix.removeSuffix("help ").removeSuffix("帮助 ")
            if (showAll && !hasPermission("command.detail"))
                return@body reply("[red]必须拥有command.detail权限才能查看完整help".with())

            helpOverwrite?.invoke(context, this@Commands, showAll, page)

            val title = if (prefix.isEmpty()) "Help" else "Help: $prefix"
            var commands = subCommands().values.toSet().sortedBy { it.name }
            if (!showAll) commands = commands.filter { info ->
                info.attrs.all { it !is Hidden || it.visible() }
            }
            reply(menu(title, commands, page, 10) {
                helpInfo(it, showAll)
            })
        }
        addSub(this)
    }

    //compatibility for [CommandInfo.body]
    @Deprecated("use CommandHandler instead", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("this.handle()")
    )
    override suspend fun invoke(p1: CommandContext) = error("use CommandHandler")

    object Root : Commands() {
        init {
            this += CommandInfo(null, "ScriptAgent", "ScriptAgent 控制指令".with(), listOf("sa")).apply {
                attr(RequirePermission("scriptAgent.admin"))
                body(controlCommand)
            }
            thisContextScript().listenTo<ScriptDisableEvent> {
                removeAll(script)
            }
        }

        var subCommandOverwrite: ((Map<String, CommandInfo>) -> Map<String, CommandInfo>)? = null
        override fun subCommands(): Map<String, CommandInfo> {
            val ret = super.subCommands()
            return subCommandOverwrite?.invoke(ret) ?: ret
        }
    }

    companion object {
        val controlCommand = Commands()

        fun CommandContext.helpInfo(it: CommandInfo, showDetail: Boolean): PlaceHoldString {
            val alias = if (it.aliases.isEmpty()) "" else it.aliases.joinToString(prefix = "(", postfix = ")")
            val detail = buildString {
                if (!showDetail) return@buildString
                if (it.script != null) append(" | ${it.script.id}")
                if (it.permission.isNotBlank()) append(" | ${it.permission}")
            }
            return "[light_yellow]{prefix}{name}[light_red]{aliases} [white]{usage}  [light_cyan]{desc}[cyan]{detail}".with(
                "prefix" to prefix, "name" to it.name, "aliases" to alias,
                "usage" to it.usage, "desc" to it.description, "detail" to detail
            )
        }

        var helpOverwrite: (suspend CommandContext.(cmds: Commands, showAll: Boolean, page: Int) -> Unit)? = null
    }
}

data class RequirePermission(val permission: String) : Commands.Hidden {
    context(CommandContext) override suspend fun visible(): Boolean = hasPermission(permission)
    context(CommandContext) override suspend fun handle() {
        if (!visible()) returnReply("[red]你没有执行该命令的权限".with())
    }
}

@ScriptDsl
inline fun Script.command(
    name: String,
    description: PlaceHoldString,
    commands: Commands = Commands.Root,
    init: CommandInfo.() -> Unit
) {
    val command = CommandInfo(this, name, description).apply(init)
    onEnable {
        commands.addSub(command)
    }
}

@ScriptDsl
inline fun Script.command(name: String, description: String, init: CommandInfo.() -> Unit) {
    command(name, description.with()) { init() }
}