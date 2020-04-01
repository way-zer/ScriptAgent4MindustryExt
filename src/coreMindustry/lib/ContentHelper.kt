package coreMindustry.lib

import arc.util.ColorCodes
import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.util.DSLBuilder
import mindustry.Vars
import mindustry.entities.type.Player
import mindustry.gen.Call

object ContentHelper{
    fun logToConsole(text:String){
        val replaced = text.replace("[green]", ColorCodes.GREEN)
            .replace("[red]", ColorCodes.LIGHT_RED)
            .replace("[yellow]", ColorCodes.YELLOW)
            .replace("[white]", ColorCodes.WHITE)
            .replace("[blue]", ColorCodes.LIGHT_BLUE)
            .replace("[]", ColorCodes.RESET)
        Log.info(replaced)
    }
}
enum class MsgType{Message, InfoMessage, InfoToast}
fun IContentScript.broadcast(text:PlaceHoldContext, type: MsgType = MsgType.Message, time:Float=10f, quite: Boolean = false, players: Iterable<Player> = Vars.playerGroup){
    if(!quite) ContentHelper.logToConsole(text.toString())
    players.forEach{
        if(it.con!=null)
            it.sendMessage(text,type, time)
    }
}
fun Player?.sendMessage(text: PlaceHoldContext, type: MsgType = MsgType.Message, time: Float = 10f){
    if(this == null) ContentHelper.logToConsole(text.toString())
    else {
        val msg = "{text}".with("text" to text,"_player" to this).toString()
        when(type){
            MsgType.Message -> Call.sendMessage(this.con,msg,null,null)
            MsgType.InfoMessage -> Call.onInfoMessage(this.con, msg)
            MsgType.InfoToast -> Call.onInfoToast(this.con, msg, time)
        }
    }
}
fun Player?.sendMessage(text: String, type: MsgType = MsgType.Message, time: Float = 10f) = sendMessage(text.with(),type,time)
fun String.with(vararg arg:Pair<String,Any>) = PlaceHoldApi.getContext(this,arg.toMap())

val Config.clientCommands by DSLBuilder.dataKeyWithDefault<CommandHandler>{Vars.netServer.clientCommands}
val Config.serverCommands by DSLBuilder.dataKeyWithDefault<CommandHandler>{ error("Can't find serverCommands")}