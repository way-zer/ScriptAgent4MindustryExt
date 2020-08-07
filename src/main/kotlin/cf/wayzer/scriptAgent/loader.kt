package cf.wayzer.scriptAgent

import cf.wayzer.script_agent.ScriptAgent

fun main() {
    if (System.getProperty("java.util.logging.SimpleFormatter.format") == null)
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1\$tF | %1\$tT | %4\$s] [%3\$s] %5\$s%6\$s%n")
    ScriptAgent.loadUseClassLoader()?.apply {
        loadClass("cf.wayzer.scriptAgent.Main").getMethod("main").invoke(null)
    }
}