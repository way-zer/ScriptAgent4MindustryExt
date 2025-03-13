@file:Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.scriptAgent.util.DSLBuilder

fun interface CommandAttr {
    /** 命令正式执行前的回调，可用于校验参数，鉴权等 */
    suspend fun CommandContext.beforeBody()
    /** 当前命令是否可用, 用于[Commands]获取子命令 */
    suspend fun visible(context: CommandContext): Boolean = true
    abstract class Param<T>(val usage: String?) : CommandAttr {
        abstract suspend fun CommandContext.resolveValue(): T
        override suspend fun CommandContext.beforeBody() {
            @Suppress("UNCHECKED_CAST")
            result = resolveValue() as (T & Any)
        }
        /** 返回解析后的参数值 */
        open fun getValue(context: CommandContext): T = context.result
        private var CommandContext.result by DSLBuilder.LateInitKey<T & Any>(usage ?: this::class.simpleName!!)
    }
}

sealed interface CommandInfoV2 {
    fun addAttr(attr: CommandAttr)
    operator fun <T : CommandAttr.Param<*>> T.provideDelegate(thisRef: Nothing?, prop: Any): T {
        addAttr(this)
        return this
    }

    operator fun <T> CommandAttr.Param<T>.getValue(thisRef: Nothing?, prop: Any): T {
        return getValue(CommandContext.Current.get())
    }

    @CommandInfo.CommandBuilder
    operator fun CommandAttr.unaryPlus() {
        addAttr(this)
    }
}


data class RequirePermission(val permission: String) : CommandAttr {
    override suspend fun visible(context: CommandContext): Boolean = context.hasPermission(permission)
    override suspend fun CommandContext.beforeBody() {
        if (!hasPermission(permission)) {
            returnReply("[red]你没有执行该命令的权限".with())
        }
    }
}

data class FlagArg(val flag: String) : CommandAttr.Param<Boolean>("[$flag]") {
    override suspend fun CommandContext.resolveValue(): Boolean = checkArg(flag)
}

data class OptionalArg<T>(val name: String, val default: T, val map: (String) -> T) : CommandAttr.Param<T>("[$name]") {
    override suspend fun CommandContext.resolveValue(): T {
        if (arg.isEmpty())
            return default
        val value = arg.first()
        arg = arg.drop(1)
        try {
            return map(value)
        } catch (e: Exception) {
            returnReply("[red]参数解析错误 {name}: {e}".with("name" to name, "e" to e))
        }
    }
}