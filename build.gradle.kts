import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "1.7.20"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "cf.wayzer"
version = "v3.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本

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
    mavenCentral()
    maven(url = "https://www.jitpack.io") {
        content {
            excludeModule("cf.wayzer", "ScriptAgent")
        }
    }
    maven("https://maven.tinylake.tk/")//ScriptAgent
}

dependencies {
    val libraryVersion = "1.9.1.6"
    val mindustryVersion = "v139"
    val pluginImplementation by configurations
    pluginImplementation("cf.wayzer:ScriptAgent:$libraryVersion")
    pluginImplementation("cf.wayzer:LibraryManager:1.4.1")
    pluginImplementation("com.github.Anuken.Mindustry:core:$mindustryVersion")

    implementation(kotlin("script-runtime"))
    implementation("cf.wayzer:ScriptAgent:$libraryVersion")
    kotlinScriptDef("cf.wayzer:ScriptAgent:$libraryVersion")

    //coreLibrary
    implementation("cf.wayzer:PlaceHoldLib:5.2")
    implementation("io.github.config4k:config4k:0.4.1")
    //coreLib/DBApi
    val exposedVersion = "0.39.2"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    //coreMindustry
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
//    implementation("com.github.TinyLake.MindustryX:core:v138.001")
    //coreMindustry/console
    implementation("org.jline:jline-terminal:3.19.0")
    implementation("org.jline:jline-reader:3.19.0")
    //coreMindustry/utilContentsOverwrite
    implementation("cf.wayzer:ContentsTweaker:v2.0.1")

    //mirai
    implementation("net.mamoe:mirai-core-api-jvm:2.10.0")
    //wayzer
    implementation("com.google.guava:guava:30.1-jre")
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xinline-classes",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xnullability-annotations=@arc.util:strict"
        )
    }
    withType<ProcessResources> {
        inputs.property("version", rootProject.version)
        filter(
            filterType = org.apache.tools.ant.filters.ReplaceTokens::class,
            properties = mapOf("tokens" to mapOf("version" to rootProject.version))
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
            "Main-Class" to "cf.wayzer.scriptAgent.GenerateMain"
        )
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent"))
            include(dependency("cf.wayzer:LibraryManager"))
        }
        doLast {
            println(archiveFile.get())
        }
    }
    val precompile = create<JavaExec>("precompile") {
        dependsOn(buildPlugin)
        group = "plugin"
        inputs.files(sourceSets.main.get().allSource)
        outputs.files("scripts/cache")

        classpath(buildPlugin.outputs.files)
        mainClass.set("cf.wayzer.scriptAgent.GenerateMain")
        if (javaVersion >= JavaVersion.VERSION_16)
            jvmArgs("--add-opens java.base/java.net=ALL-UNNAMED")
    }
    val precompileZip = create<Zip>("precompileZip") {
        dependsOn(precompile)
        group = "plugin"
        archiveClassifier.set("precompile")

        from(file("scripts/cache")) {
            include("**/*.ktc")
        }
        from(file("scripts")) {
            exclude("cache")
            exclude("metadata")
            exclude("**/*.kts")
            exclude("**/*.kt")
            exclude("**/lib")
        }
        doLast {
            println(archiveFile.get())
        }
    }

    create<Jar>("allInOneJar") {
        dependsOn(buildPlugin, precompileZip)
        group = "plugin"
        archiveClassifier.set("allInOne")
        includeEmptyDirs = false


        val metaFile = temporaryDir.resolve("PACKED")
        outputs.file(metaFile)
        doFirst {
            val ktcFiles = ZipFile(precompileZip.outputs.files.singleFile)
                .entries().asSequence()
                .filter { it.name.endsWith(".ktc") }
                .map { it.name }
            metaFile.writeText(ktcFiles.joinToString("\n"))
        }

        from(zipTree(buildPlugin.outputs.files.singleFile))
        into("scripts") {
            from(zipTree(precompileZip.outputs.files.singleFile))
            from(metaFile)
        }
    }
}