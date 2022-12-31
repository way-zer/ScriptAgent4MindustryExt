@file:Suppress("unused")

package coreLibrary.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.util.DSLBuilder

val Config.version by DSLBuilder.dataKey<String>()
val Config.mainScript by DSLBuilder.dataKey<String>()