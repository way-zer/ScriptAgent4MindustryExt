import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cf.wayzer"
version = "v3.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val loaderVersion get() = version.toString().substringBeforeLast('.')

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
    main {
        java.srcDir("scripts")
        java.exclude("cache")
    }
    create("plugin") {
        java.srcDir("plugin/src")
        resources.srcDir("plugin/res")
    }
}

repositories {
    val inChina = System.getProperty("user.timezone") in arrayOf("Asia/Shanghai", "GMT+08:00")
//    mavenLocal()
    if (inChina)
        maven(url = "https://maven.aliyun.com/repository/public")//mirror for central

    mavenCentral()
    maven(url = "https://www.jitpack.io") {
        content {
            excludeModule("cf.wayzer", "ScriptAgent")
        }
    }

    //ScriptAgent
    if (!inChina) {
        maven("https://maven.tinylake.tk/") //cloudFlare mirror
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

dependencies {
    val libraryVersion = "1.10.5.2"
    val mindustryVersion = "800fe5abd2" //v146.001
    val pluginImplementation by configurations
    pluginImplementation("cf.wayzer:ScriptAgent:$libraryVersion")
    pluginImplementation("cf.wayzer:LibraryManager:1.6")
//    pluginImplementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
    pluginImplementation("com.github.TinyLake.MindustryX:core:$mindustryVersion")

    implementation(sourceSets.getByName("plugin").output)
    implementation(kotlin("script-runtime"))
    implementation("cf.wayzer:ScriptAgent:$libraryVersion")
    kotlinScriptDef("cf.wayzer:ScriptAgent:$libraryVersion")

    //coreLibrary
    implementation("cf.wayzer:PlaceHoldLib:6.0")
    implementation("io.github.config4k:config4k:0.4.1")
    //coreLib/DBApi
    val exposedVersion = "0.40.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    //coreLib/extApi/redisApi
    implementation("redis.clients:jedis:4.3.1")
    //coreLib/extApi/mongoApi
    implementation("org.litote.kmongo:kmongo-coroutine:4.8.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    //coreMindustry
//    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
    implementation("com.github.TinyLake.MindustryX:core:$mindustryVersion")
    //coreMindustry/console
    implementation("org.jline:jline-terminal:3.21.0")
    implementation("org.jline:jline-reader:3.21.0")
    //coreMindustry/contentsTweaker
    implementation("cf.wayzer:ContentsTweaker:v3.0.1")

    //mirai
    implementation("net.mamoe:mirai-core:2.15.0")
    implementation("net.mamoe:mirai-core-utils:2.15.0")
    implementation("top.mrxiaom:qsign:1.1.0-beta")

    //wayzer
    implementation("com.google.guava:guava:30.1-jre")
}

kotlin {
    jvmToolchain(8)
}
tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xinline-classes",
            "-opt-in=kotlin.RequiresOptIn",
            "-Xnullability-annotations=@arc.util:strict"
        )
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
    create<Zip>("scriptsZip") {
        group = "plugin"
        from(sourceSets.main.get().allSource) {
            exclude("cache")
            exclude(".metadata")
        }
        archiveClassifier.set("scripts")
        doLast {
            println(archiveFile.get())
        }
    }
    val buildPlugin = create<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("buildPlugin") {
        group = "plugin"
        dependsOn("scriptsZip")
        from(sourceSets.getByName("plugin").output)
        archiveClassifier.set("")
        archiveVersion.set(rootProject.version.toString().substringBeforeLast('.'))
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
    val destPrecompile = buildDir.resolve("tmp/scripts")
    val destBuiltin = buildDir.resolve("tmp/builtinScripts")
    val precompile = create<JavaExec>("precompile") {
        dependsOn(buildPlugin)
        group = "plugin"
        classpath(buildPlugin.outputs.files)
        systemProperties["ScriptAgent.PreparePack"] = "true"
        environment("SAMAIN", "main/generate")

        inputs.files(sourceSets.main.get().allSource)
        outputs.dirs(destPrecompile, destBuiltin)
        doFirst {
            destPrecompile.deleteRecursively()
            destBuiltin.deleteRecursively()
        }
    }
    val precompileZip = create<Zip>("precompileZip") {
        dependsOn(precompile)
        group = "plugin"
        archiveClassifier.set("precompile")

        from(destPrecompile)
        doLast {
            println(archiveFile.get())
        }
    }

    create<Jar>("allInOneJar") {
        dependsOn(buildPlugin, precompileZip)
        group = "plugin"
        archiveClassifier.set("allInOne")
        includeEmptyDirs = false

        from(zipTree(buildPlugin.outputs.files.singleFile))
        from(destBuiltin) {
            into("builtin")
        }
    }
}