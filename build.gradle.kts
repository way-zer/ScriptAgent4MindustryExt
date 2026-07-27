import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    id("me.qoomon.git-versioning") version "6.4.4"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "cf.wayzer"
version = "v3.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val loaderVersion get() = version.toString()

if (projectDir.resolve(".git").isDirectory)
    gitVersioning.apply {
        refs {
            tag("v(?<version>[0-9].*)") {
                version = $$"${ref.version}"
            }
        }
        rev {
            version = $$"${commit.short}"
        }
    }

sourceSets {
    main {
        java.srcDir("loader/common")
    }
    create("mindustry").apply {
        compileClasspath += main.get().output
        configurations[compileOnlyConfigurationName].extendsFrom(configurations["runtimeClasspath"])
        java.srcDir("loader/mindustry/src")
        resources.srcDir("loader/mindustry/res")
    }
    create("bukkit").apply {
        compileClasspath += main.get().output
        configurations[compileOnlyConfigurationName].extendsFrom(configurations["runtimeClasspath"])
        java.srcDir("loader/bukkit/src")
        resources.srcDir("loader/bukkit/res")
    }
}

fun RepositoryHandler.saRepository() {
    val inChina = System.getProperty("user.timezone") in arrayOf("Asia/Shanghai", "GMT+08:00")
    maven {
        if (!inChina) {
            url = uri("https://maven.tinylake.top/")//cloudFlare mirror
        } else {
            url = uri("https://packages.aliyun.com/maven/repository/2102713-release-0NVzQH/")
            credentials {
                username = "609f6fb4aa6381038e01fdee"
                password = "h(7NRbbUWYrN"
            }
        }
        content {
            includeModule("cf.wayzer", "ScriptAgent")
        }
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        saRepository()
        maven(url = "https://www.jitpack.io") {
            content {
                excludeModule("cf.wayzer", "ScriptAgent")
            }
        }
        maven(url = "https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies.constraints {
    val mindustryVersion = "92c614c3a3" //v159.2
    api("com.github.TinyLake.MindustryX:core:$mindustryVersion")
    val bukkitVersion = "1.21.4-R0.1-SNAPSHOT"
    api("dev.folia:folia-api:$bukkitVersion")
}
dependencies {
    val libraryVersion = "2.3.3"
    api("cf.wayzer:ScriptAgent:${libraryVersion}")

    "mindustryCompileOnly"("com.github.TinyLake.MindustryX:core")
    "bukkitCompileOnly"("dev.folia:folia-api")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
tasks {
    allprojects {
        withType<JavaCompile>().configureEach {
            enabled = false
        }
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
    withType<ProcessResources>().configureEach {
        exclude("META-INF")
        filter<ReplaceTokens>(
            "tokens" to mapOf(
                "version" to loaderVersion
            )
        )
    }
    val buildPlugin by registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        group = "plugin"
        dependsOn("scriptsZip")
        from(sourceSets.main.map { it.output })
        from(sourceSets.named("mindustry").map { it.output })
        from(sourceSets.named("bukkit").map { it.output })
        archiveClassifier.set("")
        archiveVersion.set(loaderVersion)
        configurations = listOf(project.configurations.runtimeClasspath.get())
        manifest.attributes(
            "Main-Class" to "cf.wayzer.scriptAgent.standalone.LoaderKt"
        )
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent"))
            include(dependency("com.github.way-zer:LibraryManager"))
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
