package wayzer.services

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.script_agent.define.annotations.ServiceDefine

@ServiceDefine
@Suppress("RemoveRedundantQualifierName", "unused")
interface VoteService {
    val voteCommands: coreLibrary.lib.Commands
    fun cf.wayzer.script_agent.ISubScript.addSubVote(
        desc: String,
        usage: String,
        vararg aliases: String,
        body: coreLibrary.lib.CommandContext.() -> Unit
    )

    var requireNum: () -> Int
    var canVote: (mindustry.gen.Player) -> Boolean
    fun allCanVote(): List<mindustry.gen.Player>
    fun start(
        player: mindustry.gen.Player,
        voteDesc: PlaceHoldContext,
        supportSingle: Boolean = false,
        onSuccess: () -> Unit
    )
}