package teaching

import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import coreMindustry.lib.command

command("hello", "hello world~") {
    usage = "[]"
    //permission =
    aliases = listOf("hi")
    //type = CommandType.Both
    body {
        broadcast("Hello world!".with())
    }
}

/*
欢迎回来~
这期我们主要的内容是命令的编写
命令作为一个人为的触发器
是一个非常好写的东西
主要就一个函数
command(){}
在小括号内，有两个参数要填
第一个是name，也就是/后面要加什么
第二个是description，也就是在help中的命令介绍

在大括号内，也不多
usage，这条是给人看的，不是给系统看的，里面随便写。它会在help中显示出命令的用法
permission，这条是用于设置权限的，
type，命令类型，有3种，分别是Server，Client，以及both百度翻译就能看懂
aliases，用法，里面填的是除了name还能用什么触发这条指令
body，命令内容
需要注意的是，在body里面，执行命令的玩家是player!!
如果你想指定玩家，就可以直接用player!!
 */