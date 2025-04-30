package cf.wayzer.scriptAgent.standalone

import cf.wayzer.scriptAgent.*
import cf.wayzer.scriptAgent.define.LoaderApi

@OptIn(LoaderApi::class)
fun main(args: Array<String>?) {
    if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n")
    ScriptAgent.loadUseClassLoader()?.apply {
        loadClass("cf.wayzer.scriptAgent.standalone.Main")
            .getMethod("main", Array<String>::class.java)
            .invoke(null, args)
    }
}