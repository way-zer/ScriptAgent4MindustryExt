@file:MavenDepends("cf.wayzer:PlaceHoldLib:3.1", "https://dl.bintray.com/way-zer/maven/")
@file:MavenDepends("io.github.config4k:config4k:0.4.2")
@file:MavenDepends("com.typesafe:config:1.3.3")
@file:MavenDepends("org.slf4j:slf4j-simple:1.7.28")
@file:MavenDepends("org.jetbrains.exposed:exposed-core:0.24.1", single = false)
@file:MavenDepends("org.jetbrains.exposed:exposed-dao:0.24.1", single = false)
@file:MavenDepends("org.jetbrains.exposed:exposed-java-time:0.24.1", single = false)
@file:MavenDepends("org.jetbrains.exposed:exposed-jdbc:0.24.1", single = false)
@file:MavenDepends("com.h2database:h2:1.4.200", single = false)

name = "ScriptAgent 库模块"
/*
本模块实现一些平台无关的库
 */

addLibraryByClass("org.jetbrains.exposed.sql.Database")
addLibraryByClass("org.jetbrains.exposed.dao.Entity")
addLibraryByClass("org.jetbrains.exposed.sql.java-time.CurrentTimestamp")
addLibraryByClass("cf.wayzer.placehold.PlaceHoldApi")
addDefaultImport("coreLibrary.lib.*")
generateHelper()