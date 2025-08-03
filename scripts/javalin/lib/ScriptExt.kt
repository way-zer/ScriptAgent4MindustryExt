package javalin.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.util.DSLBuilder.Companion.callbackKey
import io.javalin.Javalin
import io.javalin.config.JavalinConfig

@ScriptDsl
val Script.configJavalin by callbackKey<(JavalinConfig) -> Unit>()
@ScriptDsl
val Script.webRoutes by callbackKey<Javalin.() -> Unit>()