@file:Depends("coreMindustry/utilNext", "调用菜单")

package wayzer.ext

import arc.*
import arc.struct.*
import arc.util.*
import cf.wayzer.placehold.PlaceHoldApi.with
import coreLibrary.lib.config
import coreMindustry.lib.*
import mindustry.Vars.dataDirectory
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.gen.*
import mindustry.net.Administration


val menu = contextScript<coreMindustry.UtilNext>()

val customWelcome by config.key("customWelcome", true, "是否开启自定义进服信息(中文)") {
    if (dataDirectory != null)
        Administration.Config.showConnectMessages.set(!it)
}

val type by config.key(MsgType.InfoMessage, "发送方式")
val url by config.key("https://afdian.net/@xem8k5".trimIndent(), "捐赠链接")
val Title by config.key("[slate]欢迎[blue]{player}[slate]来到[sky]cong[slate]的[sky]测试[slate]服务器".trimIndent(), "欢迎信息标题")
val welcomeText by config.key("""
    [scarlet]服务器游玩必看须知：
    [slate]一：萌新最好先通关陨石带了解各种游戏特性后再来多人联机，可以看看别的玩家的建筑来观摩观摩
    [sky]二：如果有人拆你的建筑，先再看看他后来发了些什么消息，如果是什么蓝图不太好、规划冲突等理由，大可不必无理取闹（点名批评抽水机熔毁爷新）
    [cyan]三：准备起大型蓝图或很贵的建筑时，一定要注意一下当前资源还有多少，千万不能盲目起建筑导致资源短缺
    [teal]四：对应第二条，如果你发现有人建的建筑极不恰当，可以拆除并更改，但是聊天栏必须要给出理由，由于本服成分复杂，聊天口气尽量友善点。
    [green]五：文明可是十二价值观其中的一个，聊天时不能口吐芬芳、问候家人、扣大帽、刷屏等不文明操作。如果发现有玩家确实这么干，可以投票踢出或截图并记录该玩家前三位id发给好游快爆的小恶魔封禁处理
    [acid]六：蓝图有些也是高血压蓝图，例如抽水机水电，黄沙油硅。不要乱搞，废材费电。
    [lime]七：严禁熊服，如反应堆炸核心、故意搞乱传送带、大范围故意拆除生产工厂，如发现类似玩家，可以投票踢出或截图并记录该玩家前三位id发给好游快爆的小恶魔封禁处理
    [forest]八：/votekick kick指令默认踢出15分钟
""".trimIndent(), "欢迎信息模板")

val contributorText by config.key("""
    [scarlet]服务器需要有各大捐赠者帮助才能保持运营！
    [sky]贡献者人数过多，在这里可能篇幅过长
    [cyan]请输入/ctr指令查看贡献者名单

    [scarlet]捐赠10块钱及以上就会加入贡献者名单
    [gold]捐赠648块钱及以上就可以给服务器升级配置
    [gold]同时获得至尊头衔和隐藏技能！
    [goldenrod]如果你想知道是什么技能的话
    [goldenrod]快去捐赠吧！
    
    [sky]喜报！六个贡献者技能已出炉！
    [acid]1./latum 召唤一个只能存活60秒的latum
    [acid]2./SM 你有越多的负面buff，获得的盾也越多
    [acid]3./miner 8x8的地雷部署
    [acid]4./wallkiller 粉碎周围5x5的墙壁
    [acid]5./fluid 一键在脚底下生成随机液体池
    [acid]6./flying 你作为一个地面单位，是否向往飞行的自由？
    [lime]如果你就是上文的贡献者，则需要再次找小恶魔进行二次登记获取
    [forest]如果你现在就想贡献，10块以上捐赠完后即可截图找小恶魔
    
    [sky]捐赠链接：https://afdian.net/@xem8k5
""".trimIndent(), "捐赠说明")
val contributorListText by config.key("""
[cyan]贡献者名单
[forest]感谢[cyan]9[forest]月[acid]23[forest]名赞助者
    
[gold]xkldklpppp
[white]唯兔
[orange]onecreeper
[sky]妙妙,max,爱发电用户_xsqB,爱发电用户_n3a6
爱发电用户_W7ju,40732 62021 30401,我也不知道
影曳ENDEN,教授,爱发电用户_y3vA,帅哥
爱发电用户_cdCu,橙子皮,k桑,nijiiro
111101111,qwq,小飞姬,null_114
爱发电用户_4yvX
""".trimIndent(), "捐赠者名单")
val unFinishText by config.key("""
    [scarlet]未完成
    [acid]由于技术问题，时间问题，心情问题，思想问题
    [lime]本功能暂未完成
""".trimIndent(), "未完成提示")

suspend fun welcome(p: Player) {
    menu.sendMenuBuilder<Unit>(p, 30_000, Title.with("player" to p.name).toString(), welcomeText.with().toString()) {
        add(listOf("[accent]捐赠说明" to {launch {contributor(p)}}))
        add(listOf("[red]关闭" to {}))
    }
}

suspend fun contributorList(p: Player) {
    menu.sendMenuBuilder<Unit>(p, 30_000, Title.with("player" to p.name).toString(), contributorListText.with().toString()) {
        add(listOf("[sky]返回" to {launch {contributor(p)}}))
        add(listOf("[red]关闭" to {}))
    }
}

suspend fun contributor(p: Player) {
    menu.sendMenuBuilder<Unit>(p, 30_000, Title.with("player" to p.name).toString(), contributorText.with().toString()) {
        add(listOf("[acid]打开链接" to {Call.openURI(p.con,"https://afdian.net/@xem8k5")},  //Call.openURI(url)
                   "[forest]捐赠者名单" to {launch {contributorList(p)}}))
        add(listOf("[sky]返回" to {launch {welcome(p)}},
                   "[red]关闭" to {}))

    }
}

suspend fun unFinish(p: Player) {
    menu.sendMenuBuilder<Unit>(p, 30_000, Title.with("player" to p.name).toString(), unFinishText.with().toString()) {
        add(listOf("[sky]返回主菜单" to {launch {welcome(p)}}))
        add(listOf("[red]关闭" to {}))
    }
}

listen<EventType.PlayerJoin> {
    launch { welcome(it.player) }
    var enter = 1
    if (customWelcome) {
        Call.infoPopup(
            it.player.con,
            "[coral][cyan][+]野生的 {player.name} [goldenrod]突然出现了".with("player" to it.player).toString(),
            2.013f,
            Align.left,
            -350,
            0,
            0,
            0
        )
        broadcast("[cyan][+]野生的 {player.name} [goldenrod]突然出现了".with("player" to it.player))
    }
}

listen<EventType.PlayerLeave> {
    if (customWelcome && it.player.lastText != "[Silent_Leave]") {
        Call.infoPopup(
            it.player.con,
            "[coral][-]野生的 {player.name} [brick]突然消失了".with("player" to it.player).toString(),
            2.013f,
            Align.left,
            -400,
            0,
            0,
            0
        )
        broadcast("[coral][-]野生的 {player.name} [brick]突然消失了".with("player" to it.player))
    }
}
/*
command("url", "隐藏技能") {
    usage = "[sendurl]"
    aliases = listOf("url")
    body {
        val url = arg.getOrNull(0)?: returnReply("[red]请输入url".with())
        Call.openURI(url)
    }
}
*/
command("welcomeMsg", "重载欢迎信息") {
    usage = "[welcome]"
    body {
        launch{welcome(player!!)}
    }
}