package scripts.lib

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.mindustry.MsgType
import cf.wayzer.script_agent.mindustry.sendMessage
import mindustry.Vars
import mindustry.entities.type.Player

fun IContentScript.broadcast(text:PlaceHoldContext, type:MsgType=MsgType.Message, time:Float=10f, players: Iterable<Player> = Vars.playerGroup){
    players.forEach{
        if(it.con!=null)
        it.sendMessage(text,type, time)
    }
}