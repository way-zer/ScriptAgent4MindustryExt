import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}
dependencies {
    defineModule("kcp") {
        //kcp/serialization
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    }
    defineModule("bootStrap") {}
    //不能设置为onlyLibrary，因为有子模块，IDEA会无法正确关联脚本所在模块。
    defineModule("coreLibrary"/*, onlyLibrary = true*/) {
        api("com.github.way-zer:PlaceHoldLib:v7.3")
        api("io.github.config4k:config4k:0.7.0")
        api("org.slf4j:slf4j-api:2.0.16")

        subModule("db", onlyLibrary = true) {
            val exposedVersion = "0.59.0"
            api("org.jetbrains.exposed:exposed-core:$exposedVersion")
            api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
            api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
        }

        subModule("extApi", onlyLibrary = true) {
            //coreLib/extApi/redisApi
            api("redis.clients:jedis:4.3.1")
            //coreLib/extApi/mongoApi
            api("org.litote.kmongo:kmongo-coroutine:4.8.0")
            implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
            //coreLib/extApi/KVStore
            api("com.h2database:h2-mvstore:2.3.232")
        }
    }

    defineModule("javalin") {
        dependsOnModule("coreLibrary")
        api("io.javalin:javalin:6.7.0")
    }

    defineModule("coreMindustry") {
        dependsOnModule("coreLibrary")
        api("com.github.TinyLake.MindustryX:core")
        //coreMindustry/console
        implementation("org.jline:jline-terminal:3.21.0")
        implementation("org.jline:jline-reader:3.21.0")
    }
    defineModule("scratch") {
        dependsOnModule("coreLibrary")
    }

    defineModule("wayzer") {
        dependsOnModule("coreMindustry")
        api("com.google.guava:guava:30.1-jre")
        //wayzer/ext/profiler
        implementation("tools.profiler:async-profiler:4.1")
        subModule("store") {
            dependsOnModule("coreLibrary/db")
        }
    }
    defineModule("mapScript") {
        dependsOnModule("wayzer")
    }
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xinline-classes",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xnullability-annotations=@arc.util:strict",
                "-Xcontext-parameters",
            )
        }
    }
}