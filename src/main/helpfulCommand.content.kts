package main

import arc.graphics.Colors

command("showColor","显示所有颜色"){ _, player->
    player.sendMessage(Colors.getColors().joinToString("[],") { "[#${it.value}]${it.key}" })
}