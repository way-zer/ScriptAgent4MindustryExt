
import coreLibrary.lib.with
import coreMindustry.lib.command
import mindustry.gen.*

name = "链接"

command("send", "发送链接") {
    usage = "[url]"
    permission = "cong.admin.url"
    aliases = listOf("url")
    body {
        val url = arg.getOrNull(0)?: returnReply("[red]请输入url".with())
        Call.openURI(url)
    }
}

