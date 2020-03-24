package scripts

import arc.graphics.Colors

command("showColor","显示所有颜色"){ _, p->
    p.sendMessage(Colors.getColors().joinToString("[],") { "[#${it.value}]${it.key}" })
}