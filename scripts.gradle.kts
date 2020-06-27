val kwebVersion = "0.7.0"


val scriptsCompile by configurations
dependencies {
    scriptsCompile(rootProject)
    //coreLibrary
    scriptsCompile("cf.wayzer:PlaceHoldLib:2.1.0")
    scriptsCompile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    scriptsCompile("com.h2database:h2-mvstore:1.4.200")
    scriptsCompile("io.github.config4k:config4k:0.4.1")
    scriptsCompile("org.jetbrains.exposed:exposed-core:0.24.1")
    scriptsCompile("org.jetbrains.exposed:exposed-dao:0.24.1")
    //coreWeb
    scriptsCompile("org.slf4j:slf4j-simple:1.7.28")
    scriptsCompile("io.javalin:javalin:3.8.0")
    scriptsCompile("com.fasterxml.jackson.core:jackson-databind:2.10.1")

    scriptsCompile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")
}