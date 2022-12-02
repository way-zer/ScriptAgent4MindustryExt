package teaching

import coreLibrary.lib.with
import coreMindustry.lib.broadcast

val hello = "Hello world!"

broadcast("{abcd}".with("abcd" to hello))

broadcast("$hello".with())

broadcast(hello.with())

/*
本脚本为稍微高级的hello world
用于解释with的用法

在开头,我们定义了一个常量hello,类型为String,内容为"Hello world!".

在第一个输出中,我们用的是一个传参格式
大括号中的内容可以随意写,它只是一个名字
在后面,你需要一个双引号括住大括号的内容,并用to指向一个字符串
你可以使用以下句子来理解它

输入一个      abcd,  其中  abcd  是指 hello
broadcast("{abcd}".with("abcd" to hello))


第二个输出,则是直接传入
你可以使用一个$字符加上一个量来做到直接传参
与第三种不同的是，它会强制输出，你可以输入一个Int类型，例如$five,其中five是Int类型的5
*/
val five:Int = 5
broadcast("$five".with())
/*
第三个输出,直接调用字符串
要注意的是，只能输入字符串
输入其他的是会报错的
不过可以转类型
*/
broadcast(five.toString().with())
/*
我们将在以后介绍转类型这事儿
 */