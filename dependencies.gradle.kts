val libraryVersion = "1.3.4"
val mindustryVersion = "v104"

repositories {
//    maven("http://maven.aliyun.com/nexus/content/groups/public/")
    mavenLocal()
    mavenCentral()
    jcenter()
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
    compile(kotlin("script-runtime"))
    compile("cf.wayzer:ScriptAgent:$libraryVersion")
    //coreLibrary
    compile("cf.wayzer:PlaceHoldLib:2.1.0")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    compile("org.jetbrains.exposed:exposed-core:0.24.1")
    compile("org.jetbrains.exposed:exposed-dao:0.24.1")
    compile("org.jetbrains.exposed:exposed-java-time:0.24.1")
    compile("io.github.config4k:config4k:0.4.1")
    //coreMindustry
    compile("com.github.Anuken.Mindustry:core:$mindustryVersion")
    //coreMindustry/console
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")
    compile("org.jline:jline-terminal:3.15.0")
    compile("org.jline:jline-reader:3.15.0")
    //mirai
    compile("net.mamoe:mirai-core:1.3.1")
}