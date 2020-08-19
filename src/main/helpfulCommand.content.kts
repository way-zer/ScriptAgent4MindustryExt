package main

import arc.graphics.Colors

command("showColor", "显示所有颜色") {
    reply(Colors.getColors().joinToString("[],") { "[#${it.value}]${it.key}" }.with())
}