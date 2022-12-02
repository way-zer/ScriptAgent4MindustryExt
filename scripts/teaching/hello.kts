package teaching

import coreLibrary.lib.with
import coreMindustry.lib.broadcast

broadcast("Hello world!".with())

/*
本脚本为最基础的hello world
用于解释如何输出一个文本

需要注意的是,这里在括号内的为PlaceHoldString而非String
所以，在这里写broadcast("Hello world!")会报错
错误原因是类型不匹配
解决办法是加一个with
 */
