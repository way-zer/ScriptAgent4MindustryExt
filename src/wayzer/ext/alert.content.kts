package wayzer.ext

import java.time.Duration

val type by config.key(MsgType.InfoMessage, "发送方式")
val time by config.key(Duration.ofMinutes(10)!!,"公告间隔")
val list by config.key(emptyList<String>(),"公告列表,支持颜色和变量")

var i = 0
fun broadcast(){
    i %= list.size
    broadcast(list[i].with(),type,15f)
    i++
}

onEnable{
    launch {
        delay(time.toMillis())
        broadcast()
    }
}