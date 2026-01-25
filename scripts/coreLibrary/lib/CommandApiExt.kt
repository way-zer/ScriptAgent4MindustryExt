package coreLibrary.lib

import cf.wayzer.placehold.VarString
import coreLibrary.lib.CommandContext.TabComplete

context(ctx: CommandContext)
val context: CommandContext get() = ctx

context(ctx: CommandContext)
fun reply(msg: VarString) {
    ctx.reply(msg)
}

/**Can't be call in coroutine or other context, use [reply] instead*/
@CommandInfo.CommandBuilder
context(_: CommandContext)
fun returnReply(msg: VarString): Nothing {
    reply(msg)
    CommandInfo.Return()
}

@CommandInfo.CommandBuilder
context(ctx: CommandContext)
inline fun onCompleteArg(index: Int, body: MutableList<String>.() -> Unit) {
    if (ctx is TabComplete && ctx.arg.size == index + 1) {
        ctx.result.body()
    }
}

@CommandInfo.CommandBuilder
context(_: CommandContext)
fun onComplete(index: Int, body: () -> List<String>) {
    onCompleteArg(index) {
        addAll(body())
        CommandInfo.Return()//keep old behavior
    }
}


context(context: CommandContext)
fun CommandHandler.canHandle() = context.canHandle()
context(context: CommandContext)
suspend inline fun CommandHandler.handle() = context.handle()
context(context: CommandContext)
suspend inline fun Commands.Hidden.visible() = context.visible()