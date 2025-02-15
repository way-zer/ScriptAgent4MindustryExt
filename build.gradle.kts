import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.10"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cf.wayzer"
version = "v3.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val loaderVersion get() = version.toString()

if (projectDir.resolve(".git").isDirectory)
    gitVersioning.apply(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig> {
        tag(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.VersionDescription> {
            pattern = "v(?<tagVersion>[0-9].*)"
            versionFormat = "\${tagVersion}"
        })
        commit(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.CommitVersionDescription> {
            versionFormat = "\${commit.short}-SNAPSHOT"
        })
    })

sourceSets {
    create("plugin") {
        java.srcDir("plugin/src")
        resources.srcDir("plugin/res")
    }
}

repositories {
    val inChina = System.getProperty("user.timezone") in arrayOf("Asia/Shanghai", "GMT+08:00")
//    mavenLocal()
    mavenCentral()
    maven(url = "https://www.jitpack.io") {
        content {
            excludeModule("cf.wayzer", "ScriptAgent")
        }
    }

    //ScriptAgent
    if (!inChina) {
        maven("https://maven.tinylake.top/") //cloudFlare mirror
    } else {
        maven {
            url = uri("https://packages.aliyun.com/maven/repository/2102713-release-0NVzQH/")
            credentials {
                username = "609f6fb4aa6381038e01fdee"
                password = "h(7NRbbUWYrN"
            }
        }
    }
}

//@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class Module(val id: String) {
    fun module(name: String) = Module(name)
    val api = configurations.maybeCreate("${id}Api")
    val implementation = configurations.maybeCreate("${id}Implementation")

    operator fun Configuration.invoke(module: Module) {
        extendsFrom(module.api)
        dependencies {
            this@invoke(sourceSets.getByName(module.id).output)
        }
    }
}

fun defineModule(
    name: String,
    srcDir: String = "scripts/$name",
    body: Module.() -> Unit,
) = Module(name).apply {
    sourceSets.create(id) {
        kotlin.srcDir(srcDir)
    }
    dependencies {
        api.extendsFrom(configurations.getByName("api"))
    }
    body()
}

dependencies {
    val libraryVersion = "1.11.1.3"
    val mindustryVersion = "ca40f700fb" //v146.004
    val pluginImplementation by configurations
    pluginImplementation("cf.wayzer:ScriptAgent:$libraryVersion")
    pluginImplementation("cf.wayzer:LibraryManager:1.6")
//    pluginImplementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
    pluginImplementation("com.github.TinyLake.MindustryX:core:$mindustryVersion")

    api(sourceSets.getByName("plugin").output)
    api(kotlin("script-runtime"))
    api("cf.wayzer:ScriptAgent:$libraryVersion")
    kotlinScriptDef("cf.wayzer:ScriptAgent:$libraryVersion")

    defineModule("bootStrap") {}
    defineModule("coreLibrary") {
        api("cf.wayzer:PlaceHoldLib:6.0")
        api("io.github.config4k:config4k:0.7.0")
        api("org.slf4j:slf4j-api:2.0.16")
        //coreLib/kcp/serialization
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        //coreLib/DBApi
        val exposedVersion = "0.59.0"
        api("org.jetbrains.exposed:exposed-core:$exposedVersion")
        api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
        api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
        //coreLib/extApi/redisApi
        api("redis.clients:jedis:4.3.1")
        //coreLib/extApi/mongoApi
        api("org.litote.kmongo:kmongo-coroutine:4.8.0")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        //coreLib/extApi/KVStore
        api("com.h2database:h2-mvstore:2.3.232")
    }

    defineModule("coreMindustry") {
        api(module("coreLibrary"))
//    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
        api("com.github.TinyLake.MindustryX:core:$mindustryVersion")
        //coreMindustry/console
        implementation("org.jline:jline-terminal:3.21.0")
        implementation("org.jline:jline-reader:3.21.0")
        //coreMindustry/contentsTweaker
        api("cf.wayzer:ContentsTweaker:v3.0.1")
    }
    defineModule("scratch") {
        api(module("coreLibrary"))
    }

    defineModule("wayzer") {
        api(module("coreMindustry"))
        api("com.google.guava:guava:30.1-jre")
    }
    defineModule("mapScript") {
        api(module("wayzer"))
    }
}

kotlin {
    jvmToolchain(17)
}
tasks {
    withType<JavaCompile>().configureEach {
        enabled = false
    }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xinline-classes",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xnullability-annotations=@arc.util:strict"
            )
        }
    }
    withType<ProcessResources> {
        inputs.property("version", loaderVersion)
        filter(
            filterType = org.apache.tools.ant.filters.ReplaceTokens::class,
            properties = mapOf("tokens" to mapOf("version" to loaderVersion))
        )
    }
    named<Delete>("clean") {
        delete(files("scripts/cache"))
    }
    register<Zip>("scriptsZip") {
        group = "plugin"
        from("scripts") {
            include("bootStrap/**")
            include("coreLibrary/**")
            include("coreMindustry/**")
            include("wayzer/**")
            include("mapScript/**")
        }
        archiveClassifier.set("scripts")
        doLast {
            println(archiveFile.get())
        }
    }
    val buildPlugin by registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        group = "plugin"
        dependsOn("scriptsZip")
        from(sourceSets.getByName("plugin").output)
        archiveClassifier.set("")
        archiveVersion.set(loaderVersion)
        configurations = listOf(project.configurations.getByName("pluginCompileClasspath"))
        manifest.attributes(
            "Main-Class" to "cf.wayzer.scriptAgent.standalone.LoaderKt"
        )
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent"))
            include(dependency("cf.wayzer:LibraryManager"))
        }
        doLast {
            println(archiveFile.get())
        }
    }
    val destPacked = layout.buildDirectory.file("tmp/builtin.packed.zip")
    val precompile = register<JavaExec>("precompile") {
        dependsOn(buildPlugin)
        group = "plugin"
        classpath(buildPlugin.map { it.outputs.files })
        systemProperties["ScriptAgent.PreparePack"] = "true"
        environment("SAMain", "bootStrap/generate")

        inputs.files(sourceSets.main.get().allSource)
        outputs.file(destPacked)
    }
    val precompileZip = register<Zip>("precompileZip") {
        dependsOn(precompile)
        group = "plugin"
        archiveClassifier.set("precompile.packed")

        from(zipTree(destPacked))
        doLast {
            println(archiveFile.get())
        }
    }

    register<Jar>("allInOneJar") {
        dependsOn(buildPlugin, precompileZip)
        group = "plugin"
        archiveClassifier.set("allInOne")
        includeEmptyDirs = false

        from(buildPlugin.map { zipTree(it.outputs.files.singleFile) })
        from(zipTree(destPacked)) {
            into("builtin")
        }
    }
}
