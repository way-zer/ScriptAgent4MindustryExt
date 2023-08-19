@file:Import("https://www.jitpack.io/", mavenRepository = true)
@file:Import("cf.wayzer:PlaceHoldLib:6.0", mavenDependsSingle = true)
@file:Import("io.github.config4k:config4k:0.6.0", mavenDepends = true)
@file:Import("org.slf4j:slf4j-simple:1.7.29", mavenDependsSingle = true)
@file:Import("org.slf4j:slf4j-api:1.7.29", mavenDependsSingle = true)
@file:Import("coreLibrary.lib.*", defaultImport = true)
@file:Import("coreLibrary.lib.event.*", defaultImport = true)
@file:Import("coreLibrary.lib.util.*", defaultImport = true)

package coreLibrary

import coreLibrary.lib.Color

name = "ScriptAgent 库模块"
/*
本模块实现一些平台无关的库
 */

Color//ensure init