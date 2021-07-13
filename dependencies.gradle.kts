repositories {
    mavenLocal()
    if (System.getProperty("user.timezone") == "Asia/Shanghai") {
        maven(url = "https://maven.aliyun.com/repository/public")
    }
    mavenCentral()
    if (System.getProperty("user.timezone") != "Asia/Shanghai")//ScriptAgent
        maven("https://maven.wayzer.workers.dev/")
    else {
        maven {
            url = uri("https://packages.aliyun.com/maven/repository/2102713-release-0NVzQH/")
            credentials {
                username = "609f6fb4aa6381038e01fdee"
                password = "h(7NRbbUWYrN"
            }
        }
    }
    maven(url = "https://www.jitpack.io")
}

dependencies {
    val libraryVersion = "1.7.1.1"
    val mindustryVersion = "v128"
    val pluginCompile by configurations
    pluginCompile("cf.wayzer:ScriptAgent:$libraryVersion")
    pluginCompile("cf.wayzer:LibraryManager:1.4.1")
    pluginCompile(kotlin("stdlib-jdk8"))
    pluginCompile("com.github.Anuken.Mindustry:core:$mindustryVersion")

    val compile by configurations
    val implementation by configurations
    compile(kotlin("script-runtime"))
    compile("cf.wayzer:ScriptAgent:$libraryVersion")

    //coreLibrary
    compile("cf.wayzer:PlaceHoldLib:3.1")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    compile("io.github.config4k:config4k:0.4.1")
    //coreLib/DBApi
    val exposedVersionn = "0.30.1"
    compile("org.jetbrains.exposed:exposed-core:$exposedVersionn")
    compile("org.jetbrains.exposed:exposed-dao:$exposedVersionn")
    compile("org.jetbrains.exposed:exposed-java-time:$exposedVersionn")
    //coreMindustry
    compile("com.github.Anuken.Mindustry:core:$mindustryVersion")
    //coreMindustry/console
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.1")
    compile("org.jline:jline-terminal:3.19.0")
//    compile("org.jline:jline-terminal-jansi:3.19.0")
    compile("org.jline:jline-reader:3.19.0")
    //mirai
    compile("net.mamoe:mirai-core-api-jvm:2.4.0")
    //wayzer
    implementation("com.google.guava:guava:30.1-jre")
}