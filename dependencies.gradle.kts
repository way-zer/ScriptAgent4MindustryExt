val libraryVersion = "1.4.2"
val mindustryVersion = "v115"
val exposedVersionn = "0.29.1"

repositories {
    mavenLocal()
    //maven("https://maven.aliyun.com/repository/public")
    jcenter()
    mavenCentral()
    maven(url = "https://www.jitpack.io")
    maven("https://dl.bintray.com/way-zer/maven")
}

dependencies {
    val pluginCompile by configurations
    pluginCompile("cf.wayzer:ScriptAgent:$libraryVersion")
    pluginCompile("cf.wayzer:LibraryManager:1.4")
    pluginCompile(kotlin("stdlib-jdk8"))
    pluginCompile("com.github.Anuken.Mindustry:core:$mindustryVersion")

    val compile by configurations
    val implementation by configurations
    compile(kotlin("script-runtime"))
    compile("cf.wayzer:ScriptAgent:$libraryVersion")
    //coreLibrary
    compile("cf.wayzer:PlaceHoldLib:3.1")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    compile("org.jetbrains.exposed:exposed-core:$exposedVersionn")
    compile("org.jetbrains.exposed:exposed-dao:$exposedVersionn")
    compile("org.jetbrains.exposed:exposed-java-time:$exposedVersionn")
    compile("io.github.config4k:config4k:0.4.1")
    //coreMindustry
    compile("com.github.Anuken.Mindustry:core:$mindustryVersion")
    //coreMindustry/console
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")
    compile("org.jline:jline-terminal:3.19.0")
//    compile("org.jline:jline-terminal-jansi:3.19.0")
    compile("org.jline:jline-reader:3.19.0")
    //mirai
    compile("net.mamoe:mirai-core-api-jvm:2.0-RC")
    //wayzer
    implementation("com.google.guava:guava:30.1-jre")
}