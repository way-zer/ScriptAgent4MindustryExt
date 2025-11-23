@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.placehold.VarString
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.events.ScriptDisableEvent
import cf.wayzer.scriptAgent.listenTo
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.util.menu
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.cancellation.CancellationException

sealed class CommandContext : DSLBuilder(), Cloneable {
    interface IReceiver {
        suspend fun hasPermission(node: String): Boolean
    }

    object ConsoleReceiver : IReceiver {
        override suspend fun hasPermission(node: String): Boolean = true
    }

    var receiver: IReceiver = ConsoleReceiver

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
    var reply: (msg: VarString) -> Unit = {}

    // Should not null if doing TabComplete
    @Deprecated("use TabComplete type", level = DeprecationLevel.ERROR)
    var replyTabComplete: ((list: List<String>) -> Nothing)? = null

    // Should init in RootCommand
    @set:Deprecated("implement IReceiver.hasPermission")
    var hasPermission: suspend (node: String) -> Boolean = { receiver.hasPermission(it) }

    fun subContext(): CommandContext {
        return (clone() as CommandContext).apply {
            if (arg.isEmpty()) return@apply
            prefix += arg[0] + " "
            arg = arg.subList(1, arg.size)
        }
    }

    @Deprecated("misleading name", ReplaceWith("subContext()"))
    fun getSub(): CommandContext = subContext()

    //===util===
    /**Can't be call in coroutine or other context, use [reply] instead*/
    @CommandInfo.CommandBuilder
    fun returnReply(msg: VarString): Nothing {
        reply(msg)
        CommandInfo.Return()
    }

    @CommandInfo.CommandBuilder
    inline fun onCompleteArg(index: Int, body: MutableList<String>.() -> Unit) {
        if (this is TabComplete && arg.size == index + 1) {
            result.body()
        }
    }

    @CommandInfo.CommandBuilder
    fun onComplete(index: Int, body: () -> List<String>) {
        onCompleteArg(index) {
            addAll(body())
            CommandInfo.Return()//keep old behavior
        }
    }

    inline val context get() = this

    class Command : CommandContext()
    class TabComplete : CommandContext() {
        var result = mutableListOf<String>()
    }
}

typealias CommandHandlerOld = suspend CommandContext.() -> Unit

fun interface CommandHandler {
    //Default only handle Command
    context(CommandContext) fun canHandle() = context is CommandContext.Command

    context(CommandContext) suspend fun handle()
}

@Deprecated("use CommandHandler.canHandle logic")
interface TabCompleter {
    suspend fun onComplete(context: CommandContext)
    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    @Deprecated("move to CommandContext", level = DeprecationLevel.HIDDEN)
    fun CommandContext.onComplete(index: Int, body: () -> List<String>) = onComplete(index, body)
}

@Suppress("DEPRECATION")
class CommandInfo(
    val script: Script?,
    val name: String,
    val description: VarString,
    var aliases: List<String> = emptyList(),
) : DSLBuilder(), CommandHandler, TabCompleter {
    constructor(script: Script?, name: String, description: VarString, init: CommandInfo.() -> Unit)
            : this(script, name, description) {
        init()
    }

    constructor(script: Script?, name: String, description: String, init: CommandInfo.() -> Unit = {})
            : this(script, name, description.with(), init)
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    constructor(script: Script?, name: String, description: VarString) : this(script, name, description)

    val attrs: List<CommandHandler> = mutableListOf()
    var usage: String = ""

    @Deprecated("use requirePermission(permission)")
    var permission: String = ""
    private var onComplete: CommandHandler? = null
    private var body: CommandHandler = CommandHandler {}
    private var frozen = false

    fun freeze() {
        if (frozen) return
        @Suppress("DEPRECATION")
        if (permission.isNotEmpty())
            attr(Commands.Permission(permission))
        frozen = true
    }

    /**
     * Add an attr to this command, will run before body
     */
    @CommandBuilder
    fun attr(beforeBody: CommandHandler) {
        if (frozen) error("This command is already frozen, you must add attr before body")
        (attrs as MutableList).add(beforeBody)
    }

    inline fun <reified T> attr() = attrs.filterIsInstance<T>()

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

    context(CommandContext) override fun canHandle(): Boolean =
        context is CommandContext.TabComplete || body.canHandle()

    override suspend fun onComplete(context: CommandContext) {
        //1. explicit first
        onComplete?.let {
            return context.run { it.handle() }
        }

        //2. New TabComplete logic
        with(context) {
            if (context is CommandContext.TabComplete && body.canHandle()) {
                return body.handle()
            }
        }

        //3. fallback to old logic
        (body as? TabCompleter)?.onComplete(context)
    }

    context(CommandContext) override suspend fun handle() {
        if (context is CommandContext.TabComplete)
            return onComplete(context)
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
    @Deprecated("use requirePermission(permission)")
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

    @Suppress("ObjectInheritsException")
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

@Suppress("DEPRECATION", "SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE")
open class Commands : CommandHandler, TabCompleter, CommandHandlerOld {
    fun interface Hidden : CommandHandler {
        /** 当前命令是否可用, 用于[Commands.helpCommand]处理 */
        context(CommandContext) suspend fun visible(): Boolean
        context(CommandContext) override suspend fun handle() {
            if (!visible()) returnReply("[red]该命令当前不可用".with())
        }
    }

    data class Permission(val permission: String) : Hidden {
        context(CommandContext) override suspend fun visible(): Boolean = hasPermission(permission)
        context(CommandContext) override suspend fun handle() {
            if (!visible()) returnReply("[red]你没有执行该命令的权限".with())
        }
    }

    private val watchers = mutableListOf<CommandsWatcher>()
    protected val nameMap = LinkedHashMap<String, CommandInfo>()
    open fun subCommands(): Map<String, CommandInfo> = nameMap
    fun getSub(name: String): CommandInfo? = subCommands()[name.lowercase()]

    context(CommandContext) override fun canHandle(): Boolean = true
    override suspend fun onComplete(context: CommandContext) = context.run { handle() }

    context(CommandContext) override suspend fun handle() {
        context.onComplete(0) { subCommands().keys.toList() }
        if (arg.isEmpty()) return helpCommand.handle()

        val name = arg.first()
        getSub(name)?.let {
            return subContext().run { it.handle() }
        }
        reply(
            "[red]无效指令\"{name}\",请使用 {prefix}help 查询".with("name" to name, "prefix" to prefix)
        )
    }

    protected fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
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
        watchers.forEach { it.onAdd(command) }
    }

    fun removeSub(command: CommandInfo) {
        watchers.forEach { it.onRemove(command) }
        nameMap.remove(command.name.lowercase(), command)
        command.aliases.forEach {
            nameMap.remove(it.lowercase(), command)
        }
    }

    fun removeAll(script: Script) {
        val toRemove = nameMap.values.filter { it.script == script }
        toRemove.forEach { removeSub(it) }
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

    interface CommandsWatcher {
        fun onAdd(command: CommandInfo)
        fun onRemove(command: CommandInfo)
    }

    fun addWatcher(script: Script, watcher: CommandsWatcher, fireOnRegister: Boolean = true) {
        synchronized(watchers) {
            watchers.add(watcher)
            script.onDisable {
                synchronized(watchers) {
                    watchers.remove(watcher)
                }
                if (fireOnRegister)
                    nameMap.values.toSet().forEach { watcher.onRemove(it) }
            }
        }
        if (fireOnRegister)
            nameMap.values.toSet().forEach { watcher.onAdd(it) }
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
            var commands = subCommands().let { cmds ->
                //Try to keep order if possible
                if (cmds is LinkedHashMap) {
                    val set = mutableSetOf<CommandInfo>()
                    cmds.values.mapNotNull { if (set.add(it)) it else null }
                } else {
                    cmds.values.toSet().sortedBy { it.name }
                }
            }
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
    @Deprecated(
        "use CommandHandler instead", level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("this.handle()")
    )
    override suspend fun invoke(p1: CommandContext) = error("use CommandHandler")

    object Root : Commands() {
        init {
            this += CommandInfo(thisContextScript(), "ScriptAgent", "ScriptAgent 控制指令".with(), listOf("sa")).apply {
                requirePermission("scriptAgent.admin")
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

        suspend fun tabComplete(block: CommandContext.TabComplete.() -> Unit): List<String> {
            val ctx = CommandContext.TabComplete().apply(block)
            try {
                ctx.run { handle() }
            } catch (_: CommandInfo.Return) {
            }
            return ctx.result
        }
    }

    companion object {
        val controlCommand = Commands()

        fun CommandContext.helpInfo(it: CommandInfo, showDetail: Boolean): VarString {
            val alias = if (it.aliases.isEmpty()) "" else it.aliases.joinToString(prefix = "(", postfix = ")")
            val detail = buildString {
                if (!showDetail) return@buildString
                if (it.script != null) append(" | ${it.script.id}")
                it.attr<Permission>().firstOrNull()?.let { append(" | ${it.permission}") }
            }
            return "[light_gray]{prefix}[light_yellow]{name}[gray]{aliases} [light_gray]{usage}  [light_cyan]{desc}[gray]{detail}".with(
                "prefix" to prefix, "name" to it.name, "aliases" to alias,
                "usage" to it.usage, "desc" to it.description, "detail" to detail
            )
        }

        var helpOverwrite: (suspend CommandContext.(cmds: Commands, showAll: Boolean, page: Int) -> Unit)? = null
    }
}

@ScriptDsl
inline fun Script.command(
    name: String,
    description: VarString,
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

@CommandInfo.CommandBuilder
fun CommandInfo.requirePermission(permission: String) {
    attr(Commands.Permission(permission))
}